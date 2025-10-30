package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.Objects;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractDatasetInstance<Solution_, Tuple_ extends AbstractTuple>
        implements TupleLifecycle<Tuple_>, Iterable<Tuple_> {

    private final AbstractDataset<Solution_, Tuple_> parent;
    protected final int entryStoreIndex;

    protected AbstractDatasetInstance(AbstractDataset<Solution_, Tuple_> parent, int rightMostPositionStoreIndex) {
        this.parent = Objects.requireNonNull(parent);
        this.entryStoreIndex = rightMostPositionStoreIndex;
    }

    public AbstractDataset<Solution_, Tuple_> getParent() {
        return parent;
    }

    public abstract DefaultUniqueRandomSequence<Tuple_> buildRandomSequence();

    public abstract FilteredUniqueRandomSequence<Tuple_> buildRandomSequence(Predicate<Tuple_> predicate);

    public abstract int size();

}
