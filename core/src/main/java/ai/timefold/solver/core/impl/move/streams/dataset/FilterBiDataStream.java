package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.Objects;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class FilterBiDataStream<Solution_, A, B>
        extends AbstractBiDataStream<Solution_, A, B> {

    private final BiPredicate<A, B> predicate;

    public FilterBiDataStream(DataStreamFactory<Solution_> dataStreamFactory, AbstractBiDataStream<Solution_, A, B> parent,
            BiPredicate<A, B> predicate) {
        super(dataStreamFactory, parent);
        this.predicate = Objects.requireNonNull(predicate, "The predicate cannot be null.");
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        buildHelper.<BiTuple<A, B>> putInsertUpdateRetract(this, childStreamList,
                tupleLifecycle -> TupleLifecycle.conditionally(tupleLifecycle, predicate));
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, predicate);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FilterBiDataStream<?, ?, ?> other
                && parent == other.parent
                && predicate == other.predicate;
    }

    @Override
    public String toString() {
        return "Filter() with " + childStreamList.size() + " children";
    }

}
