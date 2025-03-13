package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.Collection;
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
import ai.timefold.solver.core.impl.bavet.uni.ForEachStaticUniNode;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.SolutionExtractor;

public final class ForEachDataStream<Solution_, A>
        extends AbstractUniDataStream<Solution_, A>
        implements TupleSource {

    private final Class<A> forEachClass;
    private final Predicate<A> filter;
    private final SolutionExtractor<Solution_, A> extractor;
    private final Collection<A> source;

    public ForEachDataStream(DefaultDataStreamFactory<Solution_> dataStreamFactory, Class<A> forEachClass) {
        this(dataStreamFactory, forEachClass, (Predicate<A>) null);
    }

    public ForEachDataStream(DefaultDataStreamFactory<Solution_> dataStreamFactory, Class<A> forEachClass,
            Predicate<A> filter) {
        super(dataStreamFactory, null);
        this.forEachClass = forEachClass;
        if (forEachClass == null) {
            throw new IllegalArgumentException("The forEachClass (null) cannot be null.");
        }
        this.filter = filter;
        this.extractor = null;
        this.source = null;
    }

    public ForEachDataStream(DefaultDataStreamFactory<Solution_> dataStreamFactory, Class<A> forEachClass,
            SolutionExtractor<Solution_, A> extractor) {
        super(dataStreamFactory, null);
        this.forEachClass = forEachClass;
        this.filter = null;
        this.extractor = Objects.requireNonNull(extractor);
        this.source = null;
    }

    public ForEachDataStream(DefaultDataStreamFactory<Solution_> dataStreamFactory, Class<A> forEachClass,
            Collection<A> source) {
        super(dataStreamFactory, null);
        this.forEachClass = forEachClass;
        this.filter = null;
        this.extractor = null;
        this.source = Objects.requireNonNull(source);
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
        if (source != null) {
            return new ForEachStaticUniNode<>(forEachClass, source, tupleLifecycle, outputStoreSize);
        } else if (extractor != null) {
            return new ForEachFromSolutionUniNode<>(forEachClass, extractor, tupleLifecycle, outputStoreSize);
        } else if (filter == null) {
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
        return Objects.equals(forEachClass, that.forEachClass) && Objects.equals(filter, that.filter)
                && Objects.equals(extractor, that.extractor) && Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(forEachClass, filter, extractor, source);
    }

    @Override
    public String toString() {
        if (source != null) {
            return "Static ForEach(" + forEachClass.getSimpleName() + ") with " + childStreamList.size() + " children";
        } else if (extractor != null) {
            return "ForEach(" + forEachClass.getSimpleName() + ") from solution with " + childStreamList.size() + " children";
        } else if (filter == null) {
            return "ForEach(" + forEachClass.getSimpleName() + ") with " + childStreamList.size() + " children";
        } else {
            return "ForEach(" + forEachClass.getSimpleName() + ") with filter and " + childStreamList.size() + " children";
        }
    }

}
