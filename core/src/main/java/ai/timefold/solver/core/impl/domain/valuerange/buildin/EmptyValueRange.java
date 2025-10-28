package ai.timefold.solver.core.impl.domain.valuerange.buildin;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.sort.ValueRangeSorter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Special range for empty value ranges.
 */
@NullMarked
public final class EmptyValueRange<T> extends AbstractCountableValueRange<T> {

    private static final EmptyValueRange<Object> INSTANCE = new EmptyValueRange<>();

    @SuppressWarnings("unchecked")
    public static <T> EmptyValueRange<T> instance() {
        return (EmptyValueRange<T>) INSTANCE;
    }

    private EmptyValueRange() {
        // Intentionally empty
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public @Nullable T get(long index) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull Iterator<T> createOriginalIterator() {
        return (Iterator<T>) EmptyIterator.INSTANCE;
    }

    @Override
    public ValueRange<T> sort(ValueRangeSorter<T> sorter) {
        // Sorting operation ignored
        return this;
    }

    @Override
    public boolean contains(@Nullable T value) {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull Iterator<T> createRandomIterator(@NonNull Random workingRandom) {
        return (Iterator<T>) EmptyIterator.INSTANCE;
    }

    private static final class EmptyIterator<T> implements Iterator<T> {

        private static final EmptyIterator<Object> INSTANCE = new EmptyIterator<>();

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public T next() {
            throw new NoSuchElementException();
        }

    }

}
