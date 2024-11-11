package ai.timefold.solver.core.impl.move.dataset;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.index.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.IndexedIfExistsUniNode;
import ai.timefold.solver.core.impl.bavet.uni.UnindexedIfExistsUniNode;
import ai.timefold.solver.core.impl.move.dataset.common.BavetIfExistsDataStream;
import ai.timefold.solver.core.impl.move.dataset.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.move.dataset.common.bridge.ForeBridgeUniDataStream;

final class IfExistsUniDataStream<Solution_, A, B>
        extends AbstractUniDataStream<Solution_, A>
        implements BavetIfExistsDataStream<Solution_> {

    private final AbstractUniDataStream<Solution_, A> parentA;
    private final ForeBridgeUniDataStream<Solution_, B> parentBridgeB;
    private final boolean shouldExist;
    private final DefaultBiJoiner<A, B> joiner;
    private final BiPredicate<A, B> filtering;

    public IfExistsUniDataStream(DefaultDatasetFactory<Solution_> datasetFactory,
            AbstractUniDataStream<Solution_, A> parentA,
            ForeBridgeUniDataStream<Solution_, B> parentBridgeB,
            boolean shouldExist,
            DefaultBiJoiner<A, B> joiner, BiPredicate<A, B> filtering) {
        super(datasetFactory);
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
    public AbstractDataStream<Solution_> getLeftParent() {
        return parentA;
    }

    @Override
    public AbstractDataStream<Solution_> getRightParent() {
        return parentBridgeB;
    }

}
