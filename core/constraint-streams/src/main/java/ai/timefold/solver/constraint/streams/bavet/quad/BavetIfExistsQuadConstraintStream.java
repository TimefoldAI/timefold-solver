package ai.timefold.solver.constraint.streams.bavet.quad;

import java.util.Set;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.BavetIfExistsConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetForeBridgeUniConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.index.IndexerFactory;
import ai.timefold.solver.constraint.streams.bavet.common.index.JoinerUtils;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.common.penta.DefaultPentaJoiner;
import ai.timefold.solver.core.api.function.PentaPredicate;
import ai.timefold.solver.core.api.score.Score;

final class BavetIfExistsQuadConstraintStream<Solution_, A, B, C, D, E>
        extends BavetAbstractQuadConstraintStream<Solution_, A, B, C, D>
        implements BavetIfExistsConstraintStream<Solution_> {

    private final BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parentABCD;
    private final BavetForeBridgeUniConstraintStream<Solution_, E> parentBridgeE;

    private final boolean shouldExist;
    private final DefaultPentaJoiner<A, B, C, D, E> joiner;
    private final PentaPredicate<A, B, C, D, E> filtering;

    public BavetIfExistsQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parentABCD,
            BavetForeBridgeUniConstraintStream<Solution_, E> parentBridgeE,
            boolean shouldExist,
            DefaultPentaJoiner<A, B, C, D, E> joiner, PentaPredicate<A, B, C, D, E> filtering) {
        super(constraintFactory, parentABCD.getRetrievalSemantics());
        this.parentABCD = parentABCD;
        this.parentBridgeE = parentBridgeE;
        this.shouldExist = shouldExist;
        this.joiner = joiner;
        this.filtering = filtering;
    }

    @Override
    public boolean guaranteesDistinct() {
        return parentABCD.guaranteesDistinct();
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
        parentABCD.collectActiveConstraintStreams(constraintStreamSet);
        parentBridgeE.collectActiveConstraintStreams(constraintStreamSet);
        constraintStreamSet.add(this);
    }

    @Override
    public BavetAbstractConstraintStream<Solution_> getTupleSource() {
        return parentABCD.getTupleSource();
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        TupleLifecycle<QuadTuple<A, B, C, D>> downstream = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        IndexerFactory indexerFactory = new IndexerFactory(joiner);
        var node = indexerFactory.hasJoiners()
                ? (filtering == null ? new IndexedIfExistsQuadNode<>(shouldExist,
                        JoinerUtils.combineLeftMappings(joiner), JoinerUtils.combineRightMappings(joiner),
                        buildHelper.reserveTupleStoreIndex(parentABCD.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentABCD.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeE.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeE.getTupleSource()),
                        downstream, indexerFactory.buildIndexer(true), indexerFactory.buildIndexer(false))
                        : new IndexedIfExistsQuadNode<>(shouldExist,
                                JoinerUtils.combineLeftMappings(joiner), JoinerUtils.combineRightMappings(joiner),
                                buildHelper.reserveTupleStoreIndex(parentABCD.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentABCD.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentABCD.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeE.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeE.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeE.getTupleSource()),
                                downstream, indexerFactory.buildIndexer(true), indexerFactory.buildIndexer(false),
                                filtering))
                : (filtering == null ? new UnindexedIfExistsQuadNode<>(shouldExist,
                        buildHelper.reserveTupleStoreIndex(parentABCD.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeE.getTupleSource()), downstream)
                        : new UnindexedIfExistsQuadNode<>(shouldExist,
                                buildHelper.reserveTupleStoreIndex(parentABCD.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentABCD.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeE.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeE.getTupleSource()),
                                downstream, filtering));
        buildHelper.addNode(node, this, this, parentBridgeE);
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
        return parentABCD;
    }

    @Override
    public BavetAbstractConstraintStream<Solution_> getRightParent() {
        return parentBridgeE;
    }

}
