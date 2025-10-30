package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.common.AbstractIfExistsNode;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.IndexedIfExistsUniNode;
import ai.timefold.solver.core.impl.bavet.uni.UnindexedIfExistsUniNode;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.BiEnumeratingPredicate;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.IfExistsEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.ForeBridgeUniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.joiner.DefaultBiEnumeratingJoiner;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class IfExistsUniEnumeratingStream<Solution_, A, B>
        extends AbstractUniEnumeratingStream<Solution_, A>
        implements IfExistsEnumeratingStream<Solution_> {

    private final AbstractUniEnumeratingStream<Solution_, A> parentA;
    private final ForeBridgeUniEnumeratingStream<Solution_, B> parentBridgeB;
    private final boolean shouldExist;
    private final DefaultBiEnumeratingJoiner<A, B> joiner;
    private final @Nullable BiEnumeratingPredicate<Solution_, A, B> filtering;

    public IfExistsUniEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractUniEnumeratingStream<Solution_, A> parentA,
            ForeBridgeUniEnumeratingStream<Solution_, B> parentBridgeB, boolean shouldExist,
            DefaultBiEnumeratingJoiner<A, B> joiner,
            @Nullable BiEnumeratingPredicate<Solution_, A, B> filtering) {
        super(enumeratingStreamFactory);
        this.parentA = parentA;
        this.parentBridgeB = parentBridgeB;
        this.shouldExist = shouldExist;
        this.joiner = joiner;
        this.filtering = filtering;
    }

    @Override
    public void collectActiveEnumeratingStreams(Set<AbstractEnumeratingStream<Solution_>> enumeratingStreamSet) {
        parentA.collectActiveEnumeratingStreams(enumeratingStreamSet);
        parentBridgeB.collectActiveEnumeratingStreams(enumeratingStreamSet);
        enumeratingStreamSet.add(this);
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
        var tupleStorePositionTracker =
                buildHelper.getTupleStorePositionTracker(this, parentA.getTupleSource(), parentBridgeB.getTupleSource());
        if (indexerFactory.hasJoiners()) {
            if (isFiltering) {
                return new IndexedIfExistsUniNode<>(shouldExist, indexerFactory, downstream,
                        filtering.toBiPredicate(sessionContext.solutionView()), tupleStorePositionTracker);
            } else {
                return new IndexedIfExistsUniNode<>(shouldExist, indexerFactory, downstream, tupleStorePositionTracker);
            }
        } else if (isFiltering) {
            return new UnindexedIfExistsUniNode<>(shouldExist, downstream,
                    filtering.toBiPredicate(sessionContext.solutionView()), tupleStorePositionTracker);
        } else {
            return new UnindexedIfExistsUniNode<>(shouldExist, downstream, tupleStorePositionTracker);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IfExistsUniEnumeratingStream<?, ?, ?> that
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
    public AbstractEnumeratingStream<Solution_> getTupleSource() {
        return parentA.getTupleSource();
    }

    @Override
    public AbstractEnumeratingStream<Solution_> getLeftParent() {
        return parentA;
    }

    @Override
    public AbstractEnumeratingStream<Solution_> getRightParent() {
        return parentBridgeB;
    }
}
