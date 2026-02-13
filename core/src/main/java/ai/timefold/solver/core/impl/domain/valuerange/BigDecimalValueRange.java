package ai.timefold.solver.core.impl.domain.valuerange;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.domain.valuerange.util.ValueRangeIterator;
import ai.timefold.solver.core.impl.solver.random.RandomUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class BigDecimalValueRange extends AbstractValueRange<BigDecimal> {

    private final BigDecimal from;
    private final BigDecimal to;
    private final BigDecimal incrementUnit;

    /**
     * All parameters must have the same {@link BigDecimal#scale()}.
     *
     * @param from never null, inclusive minimum
     * @param to never null, exclusive maximum, {@code >= from}
     */
    public BigDecimalValueRange(BigDecimal from, BigDecimal to) {
        this(from, to, BigDecimal.valueOf(1L, from.scale()));
    }

    /**
     * All parameters must have the same {@link BigDecimal#scale()}.
     *
     * @param from never null, inclusive minimum
     * @param to never null, exclusive maximum, {@code >= from}
     * @param incrementUnit never null, {@code > 0}
     */
    public BigDecimalValueRange(BigDecimal from, BigDecimal to, BigDecimal incrementUnit) {
        this.from = from;
        this.to = to;
        this.incrementUnit = incrementUnit;
        int scale = from.scale();
        if (scale != to.scale()) {
            throw new IllegalArgumentException("The " + getClass().getSimpleName()
                    + " cannot have a to (" + to + ") scale (" + to.scale()
                    + ") which is different than its from (" + from + ") scale (" + scale + ").");
        }
        if (scale != incrementUnit.scale()) {
            throw new IllegalArgumentException("The " + getClass().getSimpleName()
                    + " cannot have a from (" + incrementUnit + ") scale (" + incrementUnit.scale()
                    + ") which is different than its from (" + from + ") scale (" + scale + ").");
        }
        if (to.compareTo(from) < 0) {
            throw new IllegalArgumentException("The " + getClass().getSimpleName()
                    + " cannot have a from (" + from + ") which is strictly higher than its to (" + to + ").");
        }
        if (incrementUnit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("The " + getClass().getSimpleName()
                    + " must have strictly positive incrementUnit (" + incrementUnit + ").");
        }

        if (!to.unscaledValue().subtract(from.unscaledValue()).remainder(incrementUnit.unscaledValue())
                .equals(BigInteger.ZERO)) {
            throw new IllegalArgumentException("The " + getClass().getSimpleName()
                    + "'s incrementUnit (" + incrementUnit
                    + ") must fit an integer number of times between from (" + from + ") and to (" + to + ").");
        }
    }

    @Override
    public long getSize() {
        return to.unscaledValue().subtract(from.unscaledValue()).divide(incrementUnit.unscaledValue()).longValue();
    }

    @Override
    public BigDecimal get(long index) {
        if (index < 0L || index >= getSize()) {
            throw new IndexOutOfBoundsException("The index (" + index + ") must be >= 0 and < size ("
                    + getSize() + ").");
        }
        return incrementUnit.multiply(BigDecimal.valueOf(index)).add(from);
    }

    @Override
    public boolean contains(@Nullable BigDecimal value) {
        if (value == null || value.compareTo(from) < 0 || value.compareTo(to) >= 0) {
            return false;
        }
        return value.subtract(from).remainder(incrementUnit).compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public Iterator<BigDecimal> createOriginalIterator() {
        return new OriginalBigDecimalValueRangeIterator();
    }

    private class OriginalBigDecimalValueRangeIterator extends ValueRangeIterator<BigDecimal> {

        private BigDecimal upcoming = from;

        @Override
        public boolean hasNext() {
            return upcoming.compareTo(to) < 0;
        }

        @Override
        public BigDecimal next() {
            if (upcoming.compareTo(to) >= 0) {
                throw new NoSuchElementException();
            }
            BigDecimal next = upcoming;
            upcoming = upcoming.add(incrementUnit);
            return next;
        }

    }

    @Override
    public Iterator<BigDecimal> createRandomIterator(RandomGenerator workingRandom) {
        return new RandomBigDecimalValueRangeIterator(workingRandom);
    }

    private class RandomBigDecimalValueRangeIterator extends ValueRangeIterator<BigDecimal> {

        private final RandomGenerator workingRandom;
        private final long size = getSize();

        public RandomBigDecimalValueRangeIterator(RandomGenerator workingRandom) {
            this.workingRandom = workingRandom;
        }

        @Override
        public boolean hasNext() {
            return size > 0L;
        }

        @Override
        public BigDecimal next() {
            if (size <= 0L) {
                throw new NoSuchElementException();
            }
            long index = RandomUtils.nextLong(workingRandom, size);
            return incrementUnit.multiply(BigDecimal.valueOf(index)).add(from);
        }

    }

    @Override
    public boolean equals(Object o) {
        // We do not use Objects.equals(...) due to https://bugs.openjdk.org/browse/JDK-8015417.
        if (this == o) {
            return true;
        }
        return o instanceof BigDecimalValueRange that &&
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
        return 31 * hash + incrementUnit.hashCode();
    }

    @Override
    public String toString() {
        return "[" + from + "-" + to + ")"; // Formatting: interval (mathematics) ISO 31-11
    }

}
