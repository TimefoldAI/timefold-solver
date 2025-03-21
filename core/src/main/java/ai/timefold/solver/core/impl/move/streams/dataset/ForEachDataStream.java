package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.bavet.uni.ForEachExcludingUnassignedUniNode;
import ai.timefold.solver.core.impl.bavet.uni.ForEachFromSolutionUniNode;
import ai.timefold.solver.core.impl.bavet.uni.ForEachIncludingUnassignedUniNode;
import ai.timefold.solver.core.impl.move.streams.FromSolutionValueCollectingFunction;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ForEachDataStream<Solution_, A>
        extends AbstractUniDataStream<Solution_, A>
        implements TupleSource {

    private final Class<A> forEachClass;
    private final @Nullable FromSolutionValueCollectingFunction<Solution_, A> valueCollectingFunction;
    private final @Nullable Predicate<A> filter;

    public ForEachDataStream(DataStreamFactory<Solution_> dataStreamFactory, Class<A> forEachClass) {
        this(dataStreamFactory, forEachClass, null);
    }

    public ForEachDataStream(DataStreamFactory<Solution_> dataStreamFactory, Class<A> forEachClass,
            @Nullable Predicate<A> filter) {
        super(dataStreamFactory, null);
        this.forEachClass = Objects.requireNonNull(forEachClass, "The forEachClass cannot be null.");
        this.valueCollectingFunction = null;
        this.filter = filter;
    }

    public ForEachDataStream(DataStreamFactory<Solution_> dataStreamFactory,
            FromSolutionValueCollectingFunction<Solution_, A> valueCollectingFunction) {
        super(dataStreamFactory, null);
        var function = Objects.requireNonNull(valueCollectingFunction, "The valueCollectingFunction cannot be null.");
        this.forEachClass = function.declaredClass();
        this.valueCollectingFunction = function;
        this.filter = null;
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

    private AbstractForEachUniNode<Solution_, A> getNode(TupleLifecycle<UniTuple<A>> tupleLifecycle, int outputStoreSize) {
        if (valueCollectingFunction != null) {
            return new ForEachFromSolutionUniNode<>(valueCollectingFunction, tupleLifecycle, outputStoreSize);
        } else if (filter == null) {
            return new ForEachIncludingUnassignedUniNode<>(forEachClass, tupleLifecycle, outputStoreSize);
        } else {
            return new ForEachExcludingUnassignedUniNode<>(forEachClass, filter, tupleLifecycle, outputStoreSize);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ForEachDataStream<?, ?> that &&
                Objects.equals(forEachClass, that.forEachClass) &&
                Objects.equals(valueCollectingFunction, that.valueCollectingFunction) &&
                Objects.equals(filter, that.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(forEachClass, valueCollectingFunction, filter);
    }

    @Override
    public String toString() {
        if (valueCollectingFunction != null) {
            return "ForEach(" + valueCollectingFunction + ") with " + childStreamList.size() + " children";
        } else if (filter == null) {
            return "ForEach(" + forEachClass.getSimpleName() + ") with " + childStreamList.size() + " children";
        } else {
            return "ForEach(" + forEachClass.getSimpleName() + ") with filter and " + childStreamList.size() + " children";
        }
    }

}
