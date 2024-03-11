package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.function.PentaPredicate;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetIfExistsConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.common.penta.DefaultPentaJoiner;

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
        IndexerFactory<E> indexerFactory = new IndexerFactory<>(joiner);
        var node = indexerFactory.hasJoiners()
                ? (filtering == null ? new IndexedIfExistsQuadNode<>(shouldExist,
                        indexerFactory.buildQuadLeftMapping(), indexerFactory.buildRightMapping(),
                        buildHelper.reserveTupleStoreIndex(parentABCD.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentABCD.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeE.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeE.getTupleSource()),
                        downstream, indexerFactory.buildIndexer(true), indexerFactory.buildIndexer(false))
                        : new IndexedIfExistsQuadNode<>(shouldExist,
                                indexerFactory.buildQuadLeftMapping(), indexerFactory.buildRightMapping(),
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

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        BavetIfExistsQuadConstraintStream<?, ?, ?, ?, ?, ?> that = (BavetIfExistsQuadConstraintStream<?, ?, ?, ?, ?, ?>) object;
        /*
         * Bridge streams do not implement equality because their equals() would have to point back to this stream,
         * resulting in StackOverflowError.
         * Therefore we need to check bridge parents to see where this ifExists node comes from.
         */
        return shouldExist == that.shouldExist && Objects.equals(parentABCD,
                that.parentABCD) && Objects.equals(
                        parentBridgeE.getParent(), that.parentBridgeE.getParent())
                && Objects.equals(joiner,
                        that.joiner)
                && Objects.equals(
                        filtering, that.filtering);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentABCD, parentBridgeE.getParent(), shouldExist, joiner, filtering);
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
        return parentABCD;
    }

    @Override
    public BavetAbstractConstraintStream<Solution_> getRightParent() {
        return parentBridgeE;
    }

}
