package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.bi.IndexedJoinBiNode;
import ai.timefold.solver.core.impl.bavet.bi.UnindexedJoinBiNode;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.move.streams.dataset.common.JoinDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.common.bridge.ForeBridgeUniDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.joiner.DefaultBiDataJoiner;
import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataFilter;

public final class JoinBiDataStream<Solution_, A, B> extends AbstractBiDataStream<Solution_, A, B>
        implements JoinDataStream<Solution_> {

    private final ForeBridgeUniDataStream<Solution_, A> leftParent;
    private final ForeBridgeUniDataStream<Solution_, B> rightParent;
    private final DefaultBiDataJoiner<A, B> joiner;
    private final BiDataFilter<Solution_, A, B> filtering;

    public JoinBiDataStream(DataStreamFactory<Solution_> dataStreamFactory,
            ForeBridgeUniDataStream<Solution_, A> leftParent, ForeBridgeUniDataStream<Solution_, B> rightParent,
            DefaultBiDataJoiner<A, B> joiner, BiDataFilter<Solution_, A, B> filtering) {
        super(dataStreamFactory);
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.joiner = joiner;
        this.filtering = filtering;
    }

    @Override
    public void collectActiveDataStreams(Set<AbstractDataStream<Solution_>> dataStreamSet) {
        leftParent.collectActiveDataStreams(dataStreamSet);
        rightParent.collectActiveDataStreams(dataStreamSet);
        dataStreamSet.add(this);
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        var solutionView = buildHelper.getSessionContext().solutionView();
        var filteringDataJoiner = this.filtering == null ? null : this.filtering.toBiPredicate(solutionView);
        var outputStoreSize = buildHelper.extractTupleStoreSize(this);
        TupleLifecycle<BiTuple<A, B>> downstream = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        var indexerFactory = new IndexerFactory<>(joiner.toBiJoiner());
        var node = indexerFactory.hasJoiners()
                ? new IndexedJoinBiNode<>(indexerFactory,
                        buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource()),
                        downstream, filteringDataJoiner,
                        outputStoreSize + 2, outputStoreSize, outputStoreSize + 1)
                : new UnindexedJoinBiNode<>(
                        buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource()),
                        downstream, filteringDataJoiner,
                        outputStoreSize + 2, outputStoreSize, outputStoreSize + 1);
        buildHelper.addNode(node, this, leftParent, rightParent);
    }

    @Override
    public boolean equals(Object o) {
        /*
         * Bridge streams do not implement equality because their equals() would have to point back to this stream,
         * resulting in StackOverflowError.
         * Therefore we need to check bridge parents to see where this join node comes from.
         */
        return o instanceof JoinBiDataStream<?, ?, ?> other
                && Objects.equals(leftParent.getParent(), other.leftParent.getParent())
                && Objects.equals(rightParent.getParent(), other.rightParent.getParent())
                && Objects.equals(joiner, other.joiner)
                && Objects.equals(filtering, other.filtering);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftParent.getParent(), rightParent.getParent(), joiner, filtering);
    }

    @Override
    public String toString() {
        return "BiJoin() with " + childStreamList.size() + " children";
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public AbstractDataStream<Solution_> getLeftParent() {
        return leftParent;
    }

    @Override
    public AbstractDataStream<Solution_> getRightParent() {
        return rightParent;
    }

}
