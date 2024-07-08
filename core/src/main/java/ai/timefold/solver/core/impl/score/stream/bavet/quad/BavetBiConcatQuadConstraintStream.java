package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetConcatConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeBiConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;

public final class BavetBiConcatQuadConstraintStream<Solution_, A, B, C, D>
        extends BavetAbstractQuadConstraintStream<Solution_, A, B, C, D>
        implements BavetConcatConstraintStream<Solution_> {

    private final BavetAbstractConstraintStream<Solution_> leftParent;
    private final BavetAbstractConstraintStream<Solution_> rightParent;
    private final BiFunction<A, B, C> paddingFunctionC;
    private final BiFunction<A, B, D> paddingFunctionD;
    private final ConcatNodeConstructor<A, B, C, D> nodeConstructor;

    public BavetBiConcatQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetForeBridgeBiConstraintStream<Solution_, A, B> leftParent,
            BavetForeBridgeQuadConstraintStream<Solution_, A, B, C, D> rightParent,
            BiFunction<A, B, C> paddingFunctionC, BiFunction<A, B, D> paddingFunctionD) {
        super(constraintFactory, leftParent.getRetrievalSemantics());
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.paddingFunctionC = paddingFunctionC;
        this.paddingFunctionD = paddingFunctionD;
        this.nodeConstructor = ConcatBiQuadNode::new;
    }

    public BavetBiConcatQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetForeBridgeQuadConstraintStream<Solution_, A, B, C, D> leftParent,
            BavetForeBridgeBiConstraintStream<Solution_, A, B> rightParent,
            BiFunction<A, B, C> paddingFunctionC, BiFunction<A, B, D> paddingFunctionD) {
        super(constraintFactory, leftParent.getRetrievalSemantics());
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.paddingFunctionC = paddingFunctionC;
        this.paddingFunctionD = paddingFunctionD;
        this.nodeConstructor = ConcatQuadBiNode::new;
    }

    @Override
    public boolean guaranteesDistinct() {
        /*
         * Since one of the two parents is increasing in cardinality,
         * it means its tuples must be distinct from the other parent's tuples.
         * Therefore, the guarantee can be given is both of the parents give it.
         */
        return leftParent.guaranteesDistinct() && rightParent.guaranteesDistinct();
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
        leftParent.collectActiveConstraintStreams(constraintStreamSet);
        rightParent.collectActiveConstraintStreams(constraintStreamSet);
        constraintStreamSet.add(this);
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        TupleLifecycle<QuadTuple<A, B, C, D>> downstream = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        var leftCloneStoreIndex = buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource());
        var rightCloneStoreIndex = buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource());
        var outputStoreSize = buildHelper.extractTupleStoreSize(this);
        var node = nodeConstructor.apply(paddingFunctionC, paddingFunctionD, downstream, leftCloneStoreIndex,
                rightCloneStoreIndex, outputStoreSize);
        buildHelper.addNode(node, this, leftParent, rightParent);
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var other = (BavetBiConcatQuadConstraintStream<?, ?, ?, ?, ?>) o;
        /*
         * Bridge streams do not implement equality because their equals() would have to point back to this stream,
         * resulting in StackOverflowError.
         * Therefore we need to check bridge parents to see where this concat node comes from.
         */
        return Objects.equals(leftParent.getParent(), other.leftParent.getParent())
                && Objects.equals(rightParent.getParent(), other.rightParent.getParent());
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftParent.getParent(), rightParent.getParent());
    }

    @Override
    public String toString() {
        return "BiConcat() with " + childStreamList.size() + " children";
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public BavetAbstractConstraintStream<Solution_> getLeftParent() {
        return leftParent;
    }

    @Override
    public BavetAbstractConstraintStream<Solution_> getRightParent() {
        return rightParent;
    }

    private interface ConcatNodeConstructor<A, B, C, D> {

        AbstractConcatNode<?, ?, ?> apply(BiFunction<A, B, C> paddingFunctionC, BiFunction<A, B, D> paddingFunctionD,
                TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle,
                int leftCloneStoreIndex, int rightCloneStoreIndex, int outputStoreSize);

    }

}
