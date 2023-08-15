package ai.timefold.solver.constraint.streams.bavet.uni;

import java.util.Set;
import java.util.function.BiPredicate;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.BavetIfExistsConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetForeBridgeUniConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.index.IndexerFactory;
import ai.timefold.solver.constraint.streams.bavet.common.index.JoinerUtils;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;
import ai.timefold.solver.constraint.streams.common.bi.DefaultBiJoiner;
import ai.timefold.solver.core.api.score.Score;

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
        IndexerFactory indexerFactory = new IndexerFactory(joiner);
        var node = indexerFactory.hasJoiners()
                ? (filtering == null ? new IndexedIfExistsUniNode<>(shouldExist,
                        JoinerUtils.combineLeftMappings(joiner), JoinerUtils.combineRightMappings(joiner),
                        buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                        downstream, indexerFactory.buildIndexer(true), indexerFactory.buildIndexer(false))
                        : new IndexedIfExistsUniNode<>(shouldExist,
                                JoinerUtils.combineLeftMappings(joiner), JoinerUtils.combineRightMappings(joiner),
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

    // TODO

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
