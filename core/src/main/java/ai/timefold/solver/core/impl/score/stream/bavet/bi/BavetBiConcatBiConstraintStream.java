package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetConcatConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeBiConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;

public final class BavetBiConcatBiConstraintStream<Solution_, A, B> extends BavetAbstractBiConstraintStream<Solution_, A, B>
        implements BavetConcatConstraintStream<Solution_> {

    private final BavetAbstractConstraintStream<Solution_> leftParent;
    private final BavetAbstractConstraintStream<Solution_> rightParent;
    private final ConcatNodeConstructor<A, B> nodeConstructor;

    public BavetBiConcatBiConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetForeBridgeBiConstraintStream<Solution_, A, B> leftParent,
            BavetForeBridgeBiConstraintStream<Solution_, A, B> rightParent) {
        super(constraintFactory, leftParent.getRetrievalSemantics());
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.nodeConstructor = ConcatBiBiNode::new;
    }

    @Override
    public boolean guaranteesDistinct() {
        return false; // The two parents could have the same source; guarantee impossible.
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
        TupleLifecycle<BiTuple<A, B>> downstream = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        int leftCloneStoreIndex = buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource());
        int rightCloneStoreIndex = buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource());
        int outputStoreSize = buildHelper.extractTupleStoreSize(this);
        var node = nodeConstructor.apply(downstream, leftCloneStoreIndex, rightCloneStoreIndex, outputStoreSize);
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
        BavetBiConcatBiConstraintStream<?, ?, ?> other = (BavetBiConcatBiConstraintStream<?, ?, ?>) o;
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

    private interface ConcatNodeConstructor<A, B> {

        AbstractConcatNode<?, ?, ?> apply(TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle, int leftCloneStoreIndex,
                int rightCloneStoreIndex, int outputStoreSize);

    }

}
