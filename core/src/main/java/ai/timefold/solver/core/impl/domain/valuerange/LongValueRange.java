package ai.timefold.solver.core.impl.domain.valuerange;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.domain.valuerange.util.ValueRangeIterator;
import ai.timefold.solver.core.impl.solver.random.RandomUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class LongValueRange extends AbstractValueRange<Long> {

    private final long from;
    private final long to;
    private final long incrementUnit;

    /**
     * @param from inclusive minimum
     * @param to exclusive maximum, {@code >= from}
     */
    public LongValueRange(long from, long to) {
        this(from, to, 1);
    }

    /**
     * @param from inclusive minimum
     * @param to exclusive maximum, {@code >= from}
     * @param incrementUnit {@code > 0}
     */
    public LongValueRange(long from, long to, long incrementUnit) {
        this.from = from;
        this.to = to;
        this.incrementUnit = incrementUnit;
        if (to < from) {
            throw new IllegalArgumentException("The " + getClass().getSimpleName()
                    + " cannot have a from (" + from + ") which is strictly higher than its to (" + to + ").");
        }
        if (incrementUnit <= 0L) {
            throw new IllegalArgumentException("The " + getClass().getSimpleName()
                    + " must have strictly positive incrementUnit (" + incrementUnit + ").");
        }
        if ((to - from) < 0L) { // Overflow way to detect if ((to - from) > Long.MAX_VALUE)
            throw new IllegalArgumentException("The " + getClass().getSimpleName()
                    + " cannot have a from (" + from + ") and to (" + to
                    + ") with a gap greater than Long.MAX_VALUE (" + Long.MAX_VALUE + ").");
        }
        if ((to - from) % incrementUnit != 0L) {
            throw new IllegalArgumentException("The " + getClass().getSimpleName()
                    + "'s incrementUnit (" + incrementUnit
                    + ") must fit an integer number of times between from (" + from + ") and to (" + to + ").");
        }
    }

    @Override
    public long getSize() {
        return (to - from) / incrementUnit;
    }

    @Override
    public boolean contains(@Nullable Long value) {
        if (value == null || value < from || value >= to) {
            return false;
        }
        if (incrementUnit == 1L) {
            return true;
        }
        return (value - from) % incrementUnit == 0L;
    }

    @Override
    public Long get(long index) {
        if (index < 0L || index >= getSize()) {
            throw new IndexOutOfBoundsException("The index (" + index + ") must be >= 0 and < size ("
                    + getSize() + ").");
        }
        return index * incrementUnit + from;
    }

    @Override
    public Iterator<Long> createOriginalIterator() {
        return new OriginalLongValueRangeIterator();
    }

    private class OriginalLongValueRangeIterator extends ValueRangeIterator<Long> {

        private long upcoming = from;

        @Override
        public boolean hasNext() {
            return upcoming < to;
        }

        @Override
        public Long next() {
            if (upcoming >= to) {
                throw new NoSuchElementException();
            }
            long next = upcoming;
            upcoming += incrementUnit;
            return next;
        }

    }

    @Override
    public Iterator<Long> createRandomIterator(RandomGenerator workingRandom) {
        return new RandomLongValueRangeIterator(workingRandom);
    }

    private class RandomLongValueRangeIterator extends ValueRangeIterator<Long> {

        private final RandomGenerator workingRandom;
        private final long size = getSize();

        public RandomLongValueRangeIterator(RandomGenerator workingRandom) {
            this.workingRandom = workingRandom;
        }

        @Override
        public boolean hasNext() {
            return size > 0L;
        }

        @Override
        public Long next() {
            if (size <= 0L) {
                throw new NoSuchElementException();
            }
            long index = RandomUtils.nextLong(workingRandom, size);
            return index * incrementUnit + from;
        }

    }

    @Override
    public boolean equals(Object o) {
        // We do not use Objects.equals(...) due to https://bugs.openjdk.org/browse/JDK-8015417.
        if (this == o) {
            return true;
        }
        return o instanceof LongValueRange that &&
                from == that.from &&
                to == that.to &&
                incrementUnit == that.incrementUnit;
    }

    @Override
    public int hashCode() {
        // We do not use Objects.hash(...) because it creates an array each time.
        // We do not use Objects.hashCode() due to https://bugs.openjdk.org/browse/JDK-8015417.
        var hash = 1;
        hash = 31 * hash + Long.hashCode(from);
        hash = 31 * hash + Long.hashCode(to);
        return 31 * hash + Long.hashCode(incrementUnit);
    }

    @Override
    public String toString() {
        return "[" + from + "-" + to + ")"; // Formatting: interval (mathematics) ISO 31-11
    }

}
