package ai.timefold.solver.core.impl.domain.valuerange.buildin.biginteger;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.util.ValueRangeIterator;
import ai.timefold.solver.core.impl.solver.random.RandomUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class BigIntegerValueRange extends AbstractCountableValueRange<BigInteger> {

    private final BigInteger from;
    private final BigInteger to;
    private final BigInteger incrementUnit;

    /**
     * @param from never null, inclusive minimum
     * @param to never null, exclusive maximum, {@code >= from}
     */
    public BigIntegerValueRange(BigInteger from, BigInteger to) {
        this(from, to, BigInteger.valueOf(1L));
    }

    /**
     * @param from never null, inclusive minimum
     * @param to never null, exclusive maximum, {@code >= from}
     * @param incrementUnit never null, {@code > 0}
     */
    public BigIntegerValueRange(BigInteger from, BigInteger to, BigInteger incrementUnit) {
        this.from = from;
        this.to = to;
        this.incrementUnit = incrementUnit;
        if (to.compareTo(from) < 0) {
            throw new IllegalArgumentException("The " + getClass().getSimpleName()
                    + " cannot have a from (" + from + ") which is strictly higher than its to (" + to + ").");
        }
        if (incrementUnit.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("The " + getClass().getSimpleName()
                    + " must have strictly positive incrementUnit (" + incrementUnit + ").");
        }

        if (!to.subtract(from).remainder(incrementUnit).equals(BigInteger.ZERO)) {
            throw new IllegalArgumentException("The " + getClass().getSimpleName()
                    + "'s incrementUnit (" + incrementUnit
                    + ") must fit an integer number of times between from (" + from + ") and to (" + to + ").");
        }
    }

    @Override
    public long getSize() {
        return to.subtract(from).divide(incrementUnit).longValue();
    }

    @Override
    public BigInteger get(long index) {
        if (index < 0L || index >= getSize()) {
            throw new IndexOutOfBoundsException("The index (" + index + ") must be >= 0 and < size ("
                    + getSize() + ").");
        }
        return incrementUnit.multiply(BigInteger.valueOf(index)).add(from);
    }

    @Override
    public boolean contains(@Nullable BigInteger value) {
        if (value == null || value.compareTo(from) < 0 || value.compareTo(to) >= 0) {
            return false;
        }
        return value.subtract(from).remainder(incrementUnit).compareTo(BigInteger.ZERO) == 0;
    }

    @Override
    public Iterator<BigInteger> createOriginalIterator() {
        return new OriginalBigIntegerValueRangeIterator();
    }

    private class OriginalBigIntegerValueRangeIterator extends ValueRangeIterator<BigInteger> {

        private BigInteger upcoming = from;

        @Override
        public boolean hasNext() {
            return upcoming.compareTo(to) < 0;
        }

        @Override
        public BigInteger next() {
            if (upcoming.compareTo(to) >= 0) {
                throw new NoSuchElementException();
            }
            BigInteger next = upcoming;
            upcoming = upcoming.add(incrementUnit);
            return next;
        }

    }

    @Override
    public Iterator<BigInteger> createRandomIterator(Random workingRandom) {
        return new RandomBigIntegerValueRangeIterator(workingRandom);
    }

    private class RandomBigIntegerValueRangeIterator extends ValueRangeIterator<BigInteger> {

        private final Random workingRandom;
        private final long size = getSize();

        public RandomBigIntegerValueRangeIterator(Random workingRandom) {
            this.workingRandom = workingRandom;
        }

        @Override
        public boolean hasNext() {
            return size > 0L;
        }

        @Override
        public BigInteger next() {
            if (size <= 0L) {
                throw new NoSuchElementException();
            }
            long index = RandomUtils.nextLong(workingRandom, size);
            return incrementUnit.multiply(BigInteger.valueOf(index)).add(from);
        }

    }

    @Override
    public boolean equals(Object o) {
        // We do not use Objects.equals(...) due to https://bugs.openjdk.org/browse/JDK-8015417.
        if (this == o) {
            return true;
        }
        return o instanceof BigIntegerValueRange that &&
                from.equals(that.from) &&
                to.equals(that.to) &&
                incrementUnit.equals(that.incrementUnit);
    }

    @Override
    public int hashCode() {
        // We do not use Objects.hash(...) because it creates an array each time.
        // We do not use Objects.hashCode() due to https://bugs.openjdk.org/browse/JDK-8015417.
        var hash = 1;
        hash = 31 * hash + from.hashCode();
        hash = 31 * hash + to.hashCode();
        hash = 31 * hash + incrementUnit.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "[" + from + "-" + to + ")"; // Formatting: interval (mathematics) ISO 31-11
    }

}
