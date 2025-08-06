package ai.timefold.solver.core.impl.move.streams.dataset.uni;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.common.AbstractIfExistsNode;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.IndexedIfExistsUniNode;
import ai.timefold.solver.core.impl.bavet.uni.UnindexedIfExistsUniNode;
import ai.timefold.solver.core.impl.move.streams.dataset.DataStreamFactory;
import ai.timefold.solver.core.impl.move.streams.dataset.common.AbstractDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.move.streams.dataset.common.IfExistsDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.common.bridge.ForeBridgeUniDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.joiner.DefaultBiDataJoiner;
import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataFilter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class IfExistsUniDataStream<Solution_, A, B>
        extends AbstractUniDataStream<Solution_, A>
        implements IfExistsDataStream<Solution_> {

    private final AbstractUniDataStream<Solution_, A> parentA;
    private final ForeBridgeUniDataStream<Solution_, B> parentBridgeB;
    private final boolean shouldExist;
    private final DefaultBiDataJoiner<A, B> joiner;
    private final @Nullable BiDataFilter<Solution_, A, B> filtering;

    public IfExistsUniDataStream(DataStreamFactory<Solution_> dataStreamFactory, AbstractUniDataStream<Solution_, A> parentA,
                                 ForeBridgeUniDataStream<Solution_, B> parentBridgeB, boolean shouldExist, DefaultBiDataJoiner<A, B> joiner,
                                 @Nullable BiDataFilter<Solution_, A, B> filtering) {
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
        var indexerFactory = new IndexerFactory<>(joiner.toBiJoiner());
        var node = getNode(indexerFactory, buildHelper, downstream);
        buildHelper.addNode(node, this, this, parentBridgeB);
    }

    private AbstractIfExistsNode<UniTuple<A>, B> getNode(IndexerFactory<B> indexerFactory,
            DataNodeBuildHelper<Solution_> buildHelper, TupleLifecycle<UniTuple<A>> downstream) {
        var sessionContext = buildHelper.getSessionContext();
        var isFiltering = filtering != null;
        if (indexerFactory.hasJoiners()) {
            if (isFiltering) {
                return new IndexedIfExistsUniNode<>(shouldExist, indexerFactory,
                        buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                        downstream, filtering.toBiPredicate(sessionContext.solutionView()));
            } else {
                return new IndexedIfExistsUniNode<>(shouldExist, indexerFactory,
                        buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                        downstream);
            }
        } else if (isFiltering) {
            return new UnindexedIfExistsUniNode<>(shouldExist,
                    buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                    buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                    buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                    buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()),
                    downstream, filtering.toBiPredicate(sessionContext.solutionView()));
        } else {
            return new UnindexedIfExistsUniNode<>(shouldExist,
                    buildHelper.reserveTupleStoreIndex(parentA.getTupleSource()),
                    buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource()), downstream);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IfExistsUniDataStream<?, ?, ?> that
                && shouldExist == that.shouldExist
                && Objects.equals(parentA, that.parentA)
                && Objects.equals(parentBridgeB, that.parentBridgeB)
                && Objects.equals(joiner, that.joiner)
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
