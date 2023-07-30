package ai.timefold.solver.constraint.streams.bavet.bi;

import java.util.Set;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetForeBridgeUniConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.index.IndexerFactory;
import ai.timefold.solver.constraint.streams.bavet.common.index.JoinerUtils;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.BiTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.common.tri.DefaultTriJoiner;
import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.Score;

final class BavetIfExistsBiConstraintStream<Solution_, A, B, C>
        extends BavetAbstractBiConstraintStream<Solution_, A, B> {

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
        IndexerFactory indexerFactory = new IndexerFactory(joiner);
        var node = indexerFactory.hasJoiners()
                ? (filtering == null ? new IndexedIfExistsBiNode<>(shouldExist,
                        JoinerUtils.combineLeftMappings(joiner), JoinerUtils.combineRightMappings(joiner),
                        buildHelper.reserveTupleStoreIndex(parentAB.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentAB.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeC.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeC.getTupleSource()),
                        downstream, indexerFactory.buildIndexer(true), indexerFactory.buildIndexer(false))
                        : new IndexedIfExistsBiNode<>(shouldExist,
                                JoinerUtils.combineLeftMappings(joiner), JoinerUtils.combineRightMappings(joiner),
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
        buildHelper.addNode(node, this, parentBridgeC);
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    // TODO

    @Override
    public String toString() {
        return "IfExists() with " + childStreamList.size() + " children";
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

}
