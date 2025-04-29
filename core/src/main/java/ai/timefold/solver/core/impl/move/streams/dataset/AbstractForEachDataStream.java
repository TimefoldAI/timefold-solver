package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;

import org.jspecify.annotations.NullMarked;

@NullMarked
abstract sealed class AbstractForEachDataStream<Solution_, A>
        extends AbstractUniDataStream<Solution_, A>
        implements TupleSource
        permits ForEachIncludingPinnedDataStream, ForEachExcludingPinnedDataStream, ForEachFromSolutionDataStream {

    protected final Class<A> forEachClass;

    protected AbstractForEachDataStream(DataStreamFactory<Solution_> dataStreamFactory, Class<A> forEachClass) {
        super(dataStreamFactory, null);
        this.forEachClass = Objects.requireNonNull(forEachClass);
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
