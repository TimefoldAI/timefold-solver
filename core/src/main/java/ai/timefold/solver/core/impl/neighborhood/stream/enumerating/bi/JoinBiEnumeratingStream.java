package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.bi.IndexedJoinBiNode;
import ai.timefold.solver.core.impl.bavet.bi.UnindexedJoinBiNode;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleStoreSizeTracker;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.BiEnumeratingFilter;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.JoinEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.ForeBridgeUniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.joiner.DefaultBiEnumeratingJoiner;

public final class JoinBiEnumeratingStream<Solution_, A, B> extends AbstractBiEnumeratingStream<Solution_, A, B>
        implements JoinEnumeratingStream<Solution_> {

    private final ForeBridgeUniEnumeratingStream<Solution_, A> leftParent;
    private final ForeBridgeUniEnumeratingStream<Solution_, B> rightParent;
    private final DefaultBiEnumeratingJoiner<A, B> joiner;
    private final BiEnumeratingFilter<Solution_, A, B> filtering;

    public JoinBiEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            ForeBridgeUniEnumeratingStream<Solution_, A> leftParent, ForeBridgeUniEnumeratingStream<Solution_, B> rightParent,
            DefaultBiEnumeratingJoiner<A, B> joiner, BiEnumeratingFilter<Solution_, A, B> filtering) {
        super(enumeratingStreamFactory);
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.joiner = joiner;
        this.filtering = filtering;
    }

    @Override
    public void collectActiveEnumeratingStreams(Set<AbstractEnumeratingStream<Solution_>> enumeratingStreamSet) {
        leftParent.collectActiveEnumeratingStreams(enumeratingStreamSet);
        rightParent.collectActiveEnumeratingStreams(enumeratingStreamSet);
        enumeratingStreamSet.add(this);
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        var solutionView = buildHelper.getSessionContext().solutionView();
        var filteringDataJoiner = this.filtering == null ? null : this.filtering.toBiPredicate(solutionView);
        TupleLifecycle<BiTuple<A, B>> downstream = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        var indexerFactory = new IndexerFactory<>(joiner.toBiJoiner());
        var storeSizeTracker = new TupleStoreSizeTracker(buildHelper.extractTupleStoreSize(this));
        var node = indexerFactory.hasJoiners()
                ? new IndexedJoinBiNode<>(indexerFactory,
                        buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource()),
                        downstream, filteringDataJoiner, storeSizeTracker)
                : new UnindexedJoinBiNode<>(
                        buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource()),
                        buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource()),
                        downstream, filteringDataJoiner, storeSizeTracker);
        buildHelper.addNode(node, this, leftParent, rightParent);
    }

    @Override
    public boolean equals(Object o) {
        /*
         * Bridge streams do not implement equality because their equals() would have to point back to this stream,
         * resulting in StackOverflowError.
         * Therefore we need to check bridge parents to see where this join node comes from.
         */
        return o instanceof JoinBiEnumeratingStream<?, ?, ?> other
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
    public AbstractEnumeratingStream<Solution_> getLeftParent() {
        return leftParent;
    }

    @Override
    public AbstractEnumeratingStream<Solution_> getRightParent() {
        return rightParent;
    }

}
