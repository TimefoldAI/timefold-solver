package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetConcatConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;

public final class BavetUniConcatQuadConstraintStream<Solution_, A, B, C, D>
        extends BavetAbstractQuadConstraintStream<Solution_, A, B, C, D>
        implements BavetConcatConstraintStream<Solution_> {

    private final BavetAbstractConstraintStream<Solution_> leftParent;
    private final BavetAbstractConstraintStream<Solution_> rightParent;
    private final Function<A, B> paddingFunctionB;
    private final Function<A, C> paddingFunctionC;
    private final Function<A, D> paddingFunctionD;
    private final ConcatNodeConstructor<A, B, C, D> nodeConstructor;

    public BavetUniConcatQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetForeBridgeUniConstraintStream<Solution_, A> leftParent,
            BavetForeBridgeQuadConstraintStream<Solution_, A, B, C, D> rightParent,
            Function<A, B> paddingFunctionB, Function<A, C> paddingFunctionC, Function<A, D> paddingFunctionD) {
        super(constraintFactory, leftParent.getRetrievalSemantics());
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.paddingFunctionB = paddingFunctionB;
        this.paddingFunctionC = paddingFunctionC;
        this.paddingFunctionD = paddingFunctionD;
        this.nodeConstructor = ConcatUniQuadNode::new;
    }

    public BavetUniConcatQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetForeBridgeQuadConstraintStream<Solution_, A, B, C, D> leftParent,
            BavetForeBridgeUniConstraintStream<Solution_, A> rightParent,
            Function<A, B> paddingFunctionB, Function<A, C> paddingFunctionC, Function<A, D> paddingFunctionD) {
        super(constraintFactory, leftParent.getRetrievalSemantics());
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.paddingFunctionB = paddingFunctionB;
        this.paddingFunctionC = paddingFunctionC;
        this.paddingFunctionD = paddingFunctionD;
        this.nodeConstructor = ConcatQuadUniNode::new;
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
        var node = nodeConstructor.apply(paddingFunctionB, paddingFunctionC, paddingFunctionD, downstream, leftCloneStoreIndex,
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
        var other = (BavetUniConcatQuadConstraintStream<?, ?, ?, ?, ?>) o;
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
        return "UniConcat() with " + childStreamList.size() + " children";
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

        AbstractConcatNode<?, ?, ?> apply(
                Function<A, B> paddingFunctionB, Function<A, C> paddingFunctionC, Function<A, D> paddingFunctionD,
                TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle,
                int leftCloneStoreIndex, int rightCloneStoreIndex, int outputStoreSize);

    }

}
