package ai.timefold.solver.constraint.streams.bavet.tri;

import java.util.Set;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.BavetIfExistsConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetForeBridgeUniConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.index.IndexerFactory;
import ai.timefold.solver.constraint.streams.bavet.common.index.JoinerUtils;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TriTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.common.quad.DefaultQuadJoiner;
import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.score.Score;

final class BavetIfExistsTriConstraintStream<Solution_, A, B, C, D>
        extends BavetAbstractTriConstraintStream<Solution_, A, B, C>
        implements BavetIfExistsConstraintStream<Solution_> {

    private final BavetAbstractTriConstraintStream<Solution_, A, B, C> parentABC;
    private final BavetForeBridgeUniConstraintStream<Solution_, D> parentBridgeD;

    private final boolean shouldExist;
    private final DefaultQuadJoiner<A, B, C, D> joiner;
    private final QuadPredicate<A, B, C, D> filtering;

    public BavetIfExistsTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractTriConstraintStream<Solution_, A, B, C> parentABC,
            BavetForeBridgeUniConstraintStream<Solution_, D> parentBridgeD,
            boolean shouldExist,
            DefaultQuadJoiner<A, B, C, D> joiner, QuadPredicate<A, B, C, D> filtering) {
        super(constraintFactory, parentABC.getRetrievalSemantics());
        this.parentABC = parentABC;
        this.parentBridgeD = parentBridgeD;
        this.shouldExist = shouldExist;
        this.joiner = joiner;
        this.filtering = filtering;
    }

    @Override
    public boolean guaranteesDistinct() {
        return parentABC.guaranteesDistinct();
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
        parentABC.collectActiveConstraintStreams(constraintStreamSet);
        parentBridgeD.collectActiveConstraintStreams(constraintStreamSet);
        constraintStreamSet.add(this);
    }

    @Override
    public BavetAbstractConstraintStream<Solution_> getTupleSource() {
        return parentABC.getTupleSource();
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        TupleLifecycle<TriTuple<A, B, C>> downstream = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        IndexerFactory indexerFactory = new IndexerFactory(joiner);
        var node = indexerFactory.hasJoiners()
                ? (filtering == null ? new IndexedIfExistsTriNode<>(shouldExist,
                        JoinerUtils.combineLeftMappings(joiner), JoinerUtils.combineRightMappings(joiner),
                        buildHelper.reserveTupleStoreIndex(parentABC.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentABC.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeD.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeD.getTupleSource()),
                        downstream, indexerFactory.buildIndexer(true), indexerFactory.buildIndexer(false))
                        : new IndexedIfExistsTriNode<>(shouldExist,
                                JoinerUtils.combineLeftMappings(joiner), JoinerUtils.combineRightMappings(joiner),
                                buildHelper.reserveTupleStoreIndex(parentABC.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentABC.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentABC.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeD.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeD.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeD.getTupleSource()),
                                downstream, indexerFactory.buildIndexer(true), indexerFactory.buildIndexer(false),
                                filtering))
                : (filtering == null ? new UnindexedIfExistsTriNode<>(shouldExist,
                        buildHelper.reserveTupleStoreIndex(parentABC.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeD.getTupleSource()), downstream)
                        : new UnindexedIfExistsTriNode<>(shouldExist,
                                buildHelper.reserveTupleStoreIndex(parentABC.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentABC.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeD.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeD.getTupleSource()),
                                downstream, filtering));
        buildHelper.addNode(node, this, this, parentBridgeD);
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
        return parentABC;
    }

    @Override
    public BavetAbstractConstraintStream<Solution_> getRightParent() {
        return parentBridgeD;
    }

}
