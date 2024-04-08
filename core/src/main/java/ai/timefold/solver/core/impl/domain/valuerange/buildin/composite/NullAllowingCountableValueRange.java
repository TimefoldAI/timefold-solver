package ai.timefold.solver.core.impl.domain.valuerange.buildin.composite;

import java.util.Iterator;
import java.util.Random;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.util.ValueRangeIterator;
import ai.timefold.solver.core.impl.solver.random.RandomUtils;

public final class NullAllowingCountableValueRange<T> extends AbstractCountableValueRange<T> {

    private final CountableValueRange<T> childValueRange;
    private final long size;

    public NullAllowingCountableValueRange(CountableValueRange<T> childValueRange) {
        this.childValueRange = childValueRange;
        size = childValueRange.getSize() + 1L;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public T get(long index) {
        if (index == 0) { // Consistent with the iterator.
            return null;
        } else {
            return childValueRange.get(index - 1L);
        }
    }

    @Override
    public boolean contains(T value) {
        if (value == null) {
            return true;
        }
        return childValueRange.contains(value);
    }

    @Override
    public Iterator<T> createOriginalIterator() {
        return new OriginalNullValueRangeIterator(childValueRange.createOriginalIterator());
    }

    private class OriginalNullValueRangeIterator extends ValueRangeIterator<T> {

        private boolean nullReturned = false;
        private final Iterator<T> childIterator;

        public OriginalNullValueRangeIterator(Iterator<T> childIterator) {
            this.childIterator = childIterator;
        }

        @Override
        public boolean hasNext() {
            return !nullReturned || childIterator.hasNext();
        }

        @Override
        public T next() {
            if (!nullReturned) {
                nullReturned = true;
                return null;
            } else {
                return childIterator.next();
            }
        }
    }

    @Override
    public Iterator<T> createRandomIterator(Random workingRandom) {
        return new RandomNullValueRangeIterator(workingRandom);
    }

    private class RandomNullValueRangeIterator extends ValueRangeIterator<T> {

        private final Random workingRandom;

        public RandomNullValueRangeIterator(Random workingRandom) {
            this.workingRandom = workingRandom;
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public T next() {
            long index = RandomUtils.nextLong(workingRandom, size);
            return get(index);
        }

    }

    @Override
    public String toString() {
        return "[null]∪" + childValueRange; // Formatting: interval (mathematics) ISO 31-11
    }

}
