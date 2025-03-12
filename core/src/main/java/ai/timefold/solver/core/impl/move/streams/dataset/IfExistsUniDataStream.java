package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.IndexedIfExistsUniNode;
import ai.timefold.solver.core.impl.bavet.uni.UnindexedIfExistsUniNode;
import ai.timefold.solver.core.impl.move.streams.dataset.common.BavetIfExistsDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.move.streams.dataset.common.bridge.ForeBridgeUniDataStream;

final class IfExistsUniDataStream<Solution_, A, B>
        extends AbstractUniDataStream<Solution_, A>
        implements BavetIfExistsDataStream<Solution_> {

    private final AbstractUniDataStream<Solution_, A> parentA;
    private final ForeBridgeUniDataStream<Solution_, B> parentBridgeB;
    private final boolean shouldExist;
    private final DefaultBiJoiner<A, B> joiner;
    private final BiPredicate<A, B> filtering;

    public IfExistsUniDataStream(DefaultDataStreamFactory<Solution_> dataStreamFactory,
            AbstractUniDataStream<Solution_, A> parentA, ForeBridgeUniDataStream<Solution_, B> parentBridgeB,
            boolean shouldExist, DefaultBiJoiner<A, B> joiner, BiPredicate<A, B> filtering) {
        super(dataStreamFactory);
        this.parentA = parentA;
        this.parentBridgeB = parentBridgeB;
        this.shouldExist = shouldExist;
        this.joiner = joiner;
        this.filtering = filtering;
    }

    @Override
    public void collectActiveDataStreams(Set<AbstractDataStream<Solution_>> dataStreamSet) {
        parentA.collectActiveDataStreams(dataStreamSet);
        parentBridgeB.collectActiveDataStreams(dataStreamSet);
        dataStreamSet.add(this);
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        TupleLifecycle<UniTuple<A>> downstream = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        var indexerFactory = new IndexerFactory<>(joiner);
        var node = indexerFactory.hasJoiners()
                ? (filtering == null ? new IndexedIfExistsUniNode<>(shouldExist, indexerFactory,
                        buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                        downstream)
                        : new IndexedIfExistsUniNode<>(shouldExist, indexerFactory,
                                buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                                buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                                downstream, filtering))
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof IfExistsUniDataStream<?, ?, ?> that))
            return false;
        return shouldExist == that.shouldExist && Objects.equals(parentA, that.parentA)
                && Objects.equals(parentBridgeB, that.parentBridgeB) && Objects.equals(joiner, that.joiner)
                && Objects.equals(filtering, that.filtering);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentA, parentBridgeB, shouldExist, joiner, filtering);
    }

    @Override
    public String toString() {
        return "IfExists() with " + childStreamList.size() + " children";
    }

    @Override
    public AbstractDataStream<Solution_> getTupleSource() {
        return parentA.getTupleSource();
    }

    @Override
    public BavetAbstractConstraintStream<AbstractDataStream<Solution_>> getLeftParent() {
        return null;
    }

    @Override
    public BavetAbstractConstraintStream<AbstractDataStream<Solution_>> getRightParent() {
        return null;
    }
}
