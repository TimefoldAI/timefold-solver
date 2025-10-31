package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.Objects;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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

    /**
     * This list needs to exist for iteration in random order.
     * It is expected to be optimized for fast random access and removal.
     * 
     * @param predicate A tuple will be included in the resulting list if this function returns true.
     *        If null, all tuples are included.
     * @return A copy of the list of tuples. Mutable if not empty.
     */
    public abstract UniqueRandomSequence<Tuple_> buildRandomSequence(@Nullable Predicate<Tuple_> predicate);

    public abstract int size();

}
