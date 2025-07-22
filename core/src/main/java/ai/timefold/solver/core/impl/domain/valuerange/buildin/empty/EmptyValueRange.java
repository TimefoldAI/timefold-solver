package ai.timefold.solver.core.impl.domain.valuerange.buildin.empty;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Special range for empty value ranges.
 */
@NullMarked
public final class EmptyValueRange<T> extends AbstractCountableValueRange<T> {

    public static final EmptyValueRange<Object> INSTANCE = new EmptyValueRange<>();

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
