package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.preview.api.neighborhood.function.UniNeighborhoodsFilter;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class FilterUniEnumeratingStream<Solution_, A>
        extends AbstractUniEnumeratingStream<Solution_, A> {

    private final UniNeighborhoodsFilter<Solution_, A> filter;

    public FilterUniEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractUniEnumeratingStream<Solution_, A> parent,
            UniNeighborhoodsFilter<Solution_, A> filter) {
        super(enumeratingStreamFactory, parent);
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
        } else if (o instanceof FilterUniEnumeratingStream<?, ?> other) {
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
