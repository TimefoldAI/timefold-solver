package ai.timefold.solver.core.impl.move.streams.dataset.uni;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.move.streams.dataset.DataStreamFactory;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.move.streams.maybeapi.UniDataFilter;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class FilterUniDataStream<Solution_, A>
        extends AbstractUniDataStream<Solution_, A> {

    private final UniDataFilter<Solution_, A> filter;

    public FilterUniDataStream(DataStreamFactory<Solution_> dataStreamFactory, AbstractUniDataStream<Solution_, A> parent,
            UniDataFilter<Solution_, A> filter) {
        super(dataStreamFactory, parent);
        this.filter = Objects.requireNonNull(filter, "The filter cannot be null.");
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        var predicate = filter.toPredicate(buildHelper.getSessionContext().solutionView());
        buildHelper.<UniTuple<A>> putInsertUpdateRetract(this, childStreamList,
                tupleLifecycle -> TupleLifecycle.conditionally(tupleLifecycle, predicate));
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, filter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof FilterUniDataStream<?, ?> other) {
            return parent == other.parent
                    && filter == other.filter;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Filter() with " + childStreamList.size() + " children";
    }

}
