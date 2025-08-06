package ai.timefold.solver.core.impl.move.streams.dataset.uni;

import static ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode.LifecycleOperation;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.move.streams.dataset.DataStreamFactory;
import ai.timefold.solver.core.impl.move.streams.dataset.common.AbstractDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;

import org.jspecify.annotations.NullMarked;

@NullMarked
abstract sealed class AbstractForEachDataStream<Solution_, A>
        extends AbstractUniDataStream<Solution_, A>
        implements TupleSource
        permits ForEachIncludingPinnedDataStream, ForEachFromSolutionDataStream {

    protected final Class<A> forEachClass;
    private final boolean shouldIncludeNull;

    protected AbstractForEachDataStream(DataStreamFactory<Solution_> dataStreamFactory, Class<A> forEachClass,
                                        boolean includeNull) {
        super(dataStreamFactory, null);
        this.forEachClass = Objects.requireNonNull(forEachClass);
        this.shouldIncludeNull = includeNull;
    }

    @Override
    public final void collectActiveDataStreams(Set<AbstractDataStream<Solution_>> dataStreamSet) {
        dataStreamSet.add(this);
    }

    @Override
    public final void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        TupleLifecycle<UniTuple<A>> tupleLifecycle = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        var outputStoreSize = buildHelper.extractTupleStoreSize(this);
        var node = getNode(tupleLifecycle, outputStoreSize);
        if (shouldIncludeNull && node.supports(LifecycleOperation.INSERT)) {
            node.insert(null);
        }
        buildHelper.addNode(node, this, null);
    }

    protected abstract AbstractForEachUniNode<A> getNode(TupleLifecycle<UniTuple<A>> tupleLifecycle, int outputStoreSize);

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

}
