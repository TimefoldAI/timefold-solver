package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetIfExistsConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.score.stream.common.bi.DefaultBiJoiner;

final class BavetIfExistsUniConstraintStream<Solution_, A, B>
        extends BavetAbstractUniConstraintStream<Solution_, A>
        implements BavetIfExistsConstraintStream<Solution_> {

    private final BavetAbstractUniConstraintStream<Solution_, A> parentA;
    private final BavetForeBridgeUniConstraintStream<Solution_, B> parentBridgeB;
    private final boolean shouldExist;
    private final DefaultBiJoiner<A, B> joiner;
    private final BiPredicate<A, B> filtering;

    public BavetIfExistsUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parentA,
            BavetForeBridgeUniConstraintStream<Solution_, B> parentBridgeB,
            boolean shouldExist,
            DefaultBiJoiner<A, B> joiner, BiPredicate<A, B> filtering) {
        super(constraintFactory, parentA.getRetrievalSemantics());
        this.parentA = parentA;
        this.parentBridgeB = parentBridgeB;
        this.shouldExist = shouldExist;
        this.joiner = joiner;
        this.filtering = filtering;
    }

    @Override
    public boolean guaranteesDistinct() {
        return parentA.guaranteesDistinct();
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
        parentA.collectActiveConstraintStreams(constraintStreamSet);
        parentBridgeB.collectActiveConstraintStreams(constraintStreamSet);
        constraintStreamSet.add(this);
    }

    @Override
    public BavetAbstractConstraintStream<Solution_> getTupleSource() {
        return parentA.getTupleSource();
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        TupleLifecycle<UniTuple<A>> downstream = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        IndexerFactory<B> indexerFactory = new IndexerFactory<>(joiner);
        var node = indexerFactory.hasJoiners()
                ? (filtering == null ? new IndexedIfExistsUniNode<>(shouldExist,
                        indexerFactory.buildUniLeftMapping(), indexerFactory.buildRightMapping(),
                        buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                        downstream, indexerFactory.buildIndexer(true), indexerFactory.buildIndexer(false))
                        : new IndexedIfExistsUniNode<>(shouldExist,
                                indexerFactory.buildUniLeftMapping(), indexerFactory.buildRightMapping(),
                                buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                                downstream, indexerFactory.buildIndexer(true), indexerFactory.buildIndexer(false),
                                filtering))
                : (filtering == null ? new UnindexedIfExistsUniNode<>(shouldExist,
                        buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()), downstream)
                        : new UnindexedIfExistsUniNode<>(shouldExist,
                                buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                                downstream, filtering));
        buildHelper.addNode(node, this, this, parentBridgeB);
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        BavetIfExistsUniConstraintStream<?, ?, ?> that = (BavetIfExistsUniConstraintStream<?, ?, ?>) object;
        /*
         * Bridge streams do not implement equality because their equals() would have to point back to this stream,
         * resulting in StackOverflowError.
         * Therefore we need to check bridge parents to see where this ifExists node comes from.
         */
        return shouldExist == that.shouldExist && Objects.equals(parentA,
                that.parentA) && Objects.equals(
                        parentBridgeB.getParent(), that.parentBridgeB.getParent())
                && Objects.equals(joiner,
                        that.joiner)
                && Objects.equals(
                        filtering, that.filtering);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentA, parentBridgeB.getParent(), shouldExist, joiner, filtering);
    }

    @Override
    public String toString() {
        return "IfExists() with " + childStreamList.size() + " children";
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public BavetAbstractConstraintStream<Solution_> getLeftParent() {
        return parentA;
    }

    @Override
    public BavetAbstractConstraintStream<Solution_> getRightParent() {
        return parentBridgeB;
    }

}
