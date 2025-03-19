package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.bavet.uni.ForEachExcludingUnassignedUniNode;
import ai.timefold.solver.core.impl.bavet.uni.ForEachIncludingUnassignedUniNode;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;

public final class ForEachDataStream<Solution_, A>
        extends AbstractUniDataStream<Solution_, A>
        implements TupleSource {

    private final Class<A> forEachClass;
    private final Predicate<A> filter;

    public ForEachDataStream(DataStreamFactory<Solution_> dataStreamFactory, Class<A> forEachClass) {
        this(dataStreamFactory, forEachClass, null);
    }

    public ForEachDataStream(DataStreamFactory<Solution_> dataStreamFactory, Class<A> forEachClass, Predicate<A> filter) {
        super(dataStreamFactory, null);
        this.forEachClass = Objects.requireNonNull(forEachClass, "The forEachClass cannot be null.");
        this.filter = filter;
    }

    @Override
    public void collectActiveDataStreams(Set<AbstractDataStream<Solution_>> dataStreamSet) {
        dataStreamSet.add(this);
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        TupleLifecycle<UniTuple<A>> tupleLifecycle = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        int outputStoreSize = buildHelper.extractTupleStoreSize(this);
        var node = getNode(tupleLifecycle, outputStoreSize);
        buildHelper.addNode(node, this, null);
    }

    private AbstractForEachUniNode<A> getNode(TupleLifecycle<UniTuple<A>> tupleLifecycle, int outputStoreSize) {
        if (filter == null) {
            return new ForEachIncludingUnassignedUniNode<>(forEachClass, tupleLifecycle, outputStoreSize);
        } else {
            return new ForEachExcludingUnassignedUniNode<>(forEachClass, filter, tupleLifecycle, outputStoreSize);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ForEachDataStream<?, ?> that))
            return false;
        return Objects.equals(forEachClass, that.forEachClass) && Objects.equals(filter, that.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(forEachClass, filter);
    }

    @Override
    public String toString() {
        if (filter == null) {
            return "ForEach(" + forEachClass.getSimpleName() + ") with " + childStreamList.size() + " children";
        } else {
            return "ForEach(" + forEachClass.getSimpleName() + ") with filter and " + childStreamList.size() + " children";
        }
    }

}
