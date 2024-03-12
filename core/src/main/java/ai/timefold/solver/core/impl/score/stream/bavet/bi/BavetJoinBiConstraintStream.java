package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetJoinConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.common.bi.DefaultBiJoiner;

public final class BavetJoinBiConstraintStream<Solution_, A, B> extends BavetAbstractBiConstraintStream<Solution_, A, B>
        implements BavetJoinConstraintStream<Solution_> {

    private final BavetForeBridgeUniConstraintStream<Solution_, A> leftParent;
    private final BavetForeBridgeUniConstraintStream<Solution_, B> rightParent;
    private final DefaultBiJoiner<A, B> joiner;
    private final BiPredicate<A, B> filtering;

    public BavetJoinBiConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetForeBridgeUniConstraintStream<Solution_, A> leftParent,
            BavetForeBridgeUniConstraintStream<Solution_, B> rightParent,
            DefaultBiJoiner<A, B> joiner, BiPredicate<A, B> filtering) {
        super(constraintFactory, leftParent.getRetrievalSemantics());
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.joiner = joiner;
        this.filtering = filtering;
    }

    @Override
    public boolean guaranteesDistinct() {
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
        int outputStoreSize = buildHelper.extractTupleStoreSize(this);
        TupleLifecycle<BiTuple<A, B>> downstream = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        IndexerFactory<B> indexerFactory = new IndexerFactory<>(joiner);
        var node = indexerFactory.hasJoiners()
                ? new IndexedJoinBiNode<>(
                        indexerFactory.buildUniLeftMapping(), indexerFactory.buildRightMapping(),
                        buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource()),
                        downstream, filtering, outputStoreSize + 2,
                        outputStoreSize, outputStoreSize + 1,
                        indexerFactory.buildIndexer(true), indexerFactory.buildIndexer(false))
                : new UnindexedJoinBiNode<>(
                        buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource()),
                        downstream, filtering, outputStoreSize + 2,
                        outputStoreSize, outputStoreSize + 1);
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
        BavetJoinBiConstraintStream<?, ?, ?> other = (BavetJoinBiConstraintStream<?, ?, ?>) o;
        /*
         * Bridge streams do not implement equality because their equals() would have to point back to this stream,
         * resulting in StackOverflowError.
         * Therefore we need to check bridge parents to see where this join node comes from.
         */
        return Objects.equals(leftParent.getParent(), other.leftParent.getParent())
                && Objects.equals(rightParent.getParent(), other.rightParent.getParent())
                && Objects.equals(joiner, other.joiner)
                && Objects.equals(filtering, other.filtering);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftParent.getParent(), rightParent.getParent(), joiner, filtering);
    }

    @Override
    public String toString() {
        return "BiJoin() with " + childStreamList.size() + " children";
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

}
