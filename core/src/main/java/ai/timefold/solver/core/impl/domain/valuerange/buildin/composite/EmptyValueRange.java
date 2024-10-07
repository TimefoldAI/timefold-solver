package ai.timefold.solver.core.impl.domain.valuerange.buildin.composite;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.util.ValueRangeIterator;

import org.jspecify.annotations.NonNull;

public final class EmptyValueRange<T> extends AbstractCountableValueRange<T> {

    @Override
    public long getSize() {
        return 0L;
    }

    @Override
    public T get(long index) {
        throw new IndexOutOfBoundsException("The index (" + index + ") must be >= 0 and < size ("
                + getSize() + ").");
    }

    @Override
    public boolean contains(T value) {
        return false;
    }

    @Override
    public @NonNull Iterator<T> createOriginalIterator() {
        return new EmptyValueRangeIterator();
    }

    @Override
    public @NonNull Iterator<T> createRandomIterator(@NonNull Random workingRandom) {
        return new EmptyValueRangeIterator();
    }

    private class EmptyValueRangeIterator extends ValueRangeIterator<T> {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public T next() {
            throw new NoSuchElementException();
        }

    }

    @Override
    public String toString() {
        return "[]"; // Formatting: interval (mathematics) ISO 31-11
    }

}
