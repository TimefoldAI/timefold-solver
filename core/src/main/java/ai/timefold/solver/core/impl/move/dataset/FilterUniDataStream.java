package ai.timefold.solver.core.impl.move.dataset;

import java.util.Objects;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.ConditionalUniTupleLifecycle;
import ai.timefold.solver.core.impl.move.dataset.common.DataNodeBuildHelper;

final class FilterUniDataStream<Solution_, A>
        extends AbstractUniDataStream<Solution_, A> {

    private final Predicate<A> predicate;

    public FilterUniDataStream(DefaultDatasetFactory<Solution_> datasetFactory,
            AbstractUniDataStream<Solution_, A> parent, Predicate<A> predicate) {
        super(datasetFactory, parent);
        this.predicate = predicate;
        if (predicate == null) {
            throw new IllegalArgumentException("The predicate (null) cannot be null.");
        }
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        buildHelper.<UniTuple<A>> putInsertUpdateRetract(this, childStreamList,
                tupleLifecycle -> new ConditionalUniTupleLifecycle<>(predicate, tupleLifecycle));
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
