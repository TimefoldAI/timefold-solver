package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.Objects;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class FilterUniDataStream<Solution_, A>
        extends AbstractUniDataStream<Solution_, A> {

    private final Predicate<A> predicate;

    public FilterUniDataStream(DataStreamFactory<Solution_> dataStreamFactory, AbstractUniDataStream<Solution_, A> parent,
            Predicate<A> predicate) {
        super(dataStreamFactory, parent);
        this.predicate = Objects.requireNonNull(predicate, "The predicate cannot be null.");
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        buildHelper.<UniTuple<A>> putInsertUpdateRetract(this, childStreamList,
                tupleLifecycle -> TupleLifecycle.conditionally(tupleLifecycle, predicate));
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, predicate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof FilterUniDataStream<?, ?> other) {
            return parent == other.parent
                    && predicate == other.predicate;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Filter() with " + childStreamList.size() + " children";
    }

}
