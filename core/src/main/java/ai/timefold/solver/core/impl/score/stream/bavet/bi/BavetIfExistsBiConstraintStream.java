package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetIfExistsConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.common.tri.DefaultTriJoiner;

final class BavetIfExistsBiConstraintStream<Solution_, A, B, C>
        extends BavetAbstractBiConstraintStream<Solution_, A, B>
        implements BavetIfExistsConstraintStream<Solution_> {

    private final BavetAbstractBiConstraintStream<Solution_, A, B> parentAB;
    private final BavetForeBridgeUniConstraintStream<Solution_, C> parentBridgeC;

    private final boolean shouldExist;
    private final DefaultTriJoiner<A, B, C> joiner;
    private final TriPredicate<A, B, C> filtering;

    public BavetIfExistsBiConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractBiConstraintStream<Solution_, A, B> parentAB,
            BavetForeBridgeUniConstraintStream<Solution_, C> parentBridgeC,
            boolean shouldExist,
            DefaultTriJoiner<A, B, C> joiner, TriPredicate<A, B, C> filtering) {
        super(constraintFactory, parentAB.getRetrievalSemantics());
        this.parentAB = parentAB;
        this.parentBridgeC = parentBridgeC;
        this.shouldExist = shouldExist;
        this.joiner = joiner;
        this.filtering = filtering;
    }

    @Override
    public boolean guaranteesDistinct() {
        return parentAB.guaranteesDistinct();
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
        parentAB.collectActiveConstraintStreams(constraintStreamSet);
        parentBridgeC.collectActiveConstraintStreams(constraintStreamSet);
        constraintStreamSet.add(this);
    }

    @Override
    public BavetAbstractConstraintStream<Solution_> getTupleSource() {
        return parentAB.getTupleSource();
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        TupleLifecycle<BiTuple<A, B>> downstream = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        IndexerFactory<C> indexerFactory = new IndexerFactory<>(joiner);
        var node = indexerFactory.hasJoiners()
                ? (filtering == null ? new IndexedIfExistsBiNode<>(shouldExist,
                        indexerFactory.buildBiLeftMapping(), indexerFactory.buildRightMapping(),
                        buildHelper.reserveTupleStoreIndex(parentAB.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentAB.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeC.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeC.getTupleSource()),
                        downstream, indexerFactory.buildIndexer(true), indexerFactory.buildIndexer(false))
                        : new IndexedIfExistsBiNode<>(shouldExist,
                                indexerFactory.buildBiLeftMapping(), indexerFactory.buildRightMapping(),
                                buildHelper.reserveTupleStoreIndex(parentAB.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentAB.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentAB.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeC.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeC.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeC.getTupleSource()),
                                downstream, indexerFactory.buildIndexer(true), indexerFactory.buildIndexer(false),
                                filtering))
                : (filtering == null ? new UnindexedIfExistsBiNode<>(shouldExist,
                        buildHelper.reserveTupleStoreIndex(parentAB.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeC.getTupleSource()), downstream)
                        : new UnindexedIfExistsBiNode<>(shouldExist,
                                buildHelper.reserveTupleStoreIndex(parentAB.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentAB.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeC.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeC.getTupleSource()),
                                downstream, filtering));
        buildHelper.addNode(node, this, this, parentBridgeC);
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
        BavetIfExistsBiConstraintStream<?, ?, ?, ?> that = (BavetIfExistsBiConstraintStream<?, ?, ?, ?>) object;
        /*
         * Bridge streams do not implement equality because their equals() would have to point back to this stream,
         * resulting in StackOverflowError.
         * Therefore we need to check bridge parents to see where this ifExists node comes from.
         */
        return shouldExist == that.shouldExist && Objects.equals(parentAB,
                that.parentAB) && Objects.equals(
                        parentBridgeC.getParent(), that.parentBridgeC.getParent())
                && Objects.equals(joiner,
                        that.joiner)
                && Objects.equals(
                        filtering, that.filtering);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentAB, parentBridgeC.getParent(), shouldExist, joiner, filtering);
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
        return parentAB;
    }

    @Override
    public BavetAbstractConstraintStream<Solution_> getRightParent() {
        return parentBridgeC;
    }

}
