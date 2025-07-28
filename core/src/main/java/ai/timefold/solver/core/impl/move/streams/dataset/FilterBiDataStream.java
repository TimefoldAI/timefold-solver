package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataFilter;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class FilterBiDataStream<Solution_, A, B>
        extends AbstractBiDataStream<Solution_, A, B> {

    private final BiDataFilter<Solution_, A, B> filter;

    public FilterBiDataStream(DataStreamFactory<Solution_> dataStreamFactory, AbstractBiDataStream<Solution_, A, B> parent,
            BiDataFilter<Solution_, A, B> filter) {
        super(dataStreamFactory, parent);
        this.filter = Objects.requireNonNull(filter, "The predicate cannot be null.");
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        var predicate = filter.toBiPredicate(buildHelper.getSessionContext().solutionView());
        buildHelper.<BiTuple<A, B>> putInsertUpdateRetract(this, childStreamList,
                tupleLifecycle -> TupleLifecycle.conditionally(tupleLifecycle, predicate));
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, filter);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FilterBiDataStream<?, ?, ?> other
                && parent == other.parent
                && filter == other.filter;
    }

    @Override
    public String toString() {
        return "Filter() with " + childStreamList.size() + " children";
    }

}
