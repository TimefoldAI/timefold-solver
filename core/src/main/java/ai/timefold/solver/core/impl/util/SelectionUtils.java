package ai.timefold.solver.core.impl.util;

import java.util.function.IntToLongFunction;
import java.util.function.LongSupplier;
import java.util.function.ToLongFunction;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Size arithmetic and bail-out budgets for selectors and neighborhoods.
 * <p>
 * Sizes saturate at {@link Long#MAX_VALUE}, which means "at least this many".
 * <p>
 * A bail-out is how many consecutive rejected selections a filtering iterator or selector tries
 * before it gives up on a never-ending source.
 * Trying 10 times the source's size gives reasonable confidence that no acceptable selection was missed.
 * The budget is capped, so that never-ending random iteration always ends in practice.
 */
@NullMarked
public final class SelectionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SelectionUtils.class);

    public static final long MULTIPLIER = 10L;
    /**
     * Used when the source's size is unknown or negative.
     */
    public static final long UNKNOWN_SIZE_BAIL_OUT = Short.MAX_VALUE * MULTIPLIER;
    /**
     * The largest budget; about one second of filter calls.
     */
    public static final long MAX_BAIL_OUT = 10_000_000L;

    /**
     * @return {@code a + b}, or {@link Long#MAX_VALUE} or {@link Long#MIN_VALUE} if that overflows
     */
    public static long sum(long a, long b) {
        var result = a + b;
        if (((a ^ result) & (b ^ result)) < 0) {
            return a < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return result;
    }

    /**
     * @return the sum of the sizes of all items; stops at {@link Long#MAX_VALUE}
     */
    public static <T> long sumOf(Iterable<T> items, ToLongFunction<? super T> size) {
        var total = 0L;
        for (var item : items) {
            total = sum(total, size.applyAsLong(item));
            if (total == Long.MAX_VALUE) {
                break;
            }
        }
        return total;
    }

    /**
     * @return the sum of {@code size(i)} for {@code from <= i <= to}; stops at {@link Long#MAX_VALUE}
     */
    public static long sumOf(int from, int to, IntToLongFunction size) {
        var total = 0L;
        for (var i = (long) from; i <= to && total != Long.MAX_VALUE; i++) {
            total = sum(total, size.applyAsLong((int) i));
        }
        return total;
    }

    /**
     * @return {@code a * b}, or {@link Long#MAX_VALUE} or {@link Long#MIN_VALUE} if that overflows
     */
    public static long product(long a, long b) {
        var high = Math.multiplyHigh(a, b);
        var low = a * b;
        if ((high == 0 && low >= 0) || (high == -1 && low < 0)) {
            return low;
        }
        return (a ^ b) < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
    }

    /**
     * @return {@code a * b * c}, saturated as {@link #product(long, long)}
     */
    public static long product(long a, long b, long c) {
        return product(product(a, b), c);
    }

    /**
     * @return the number of ordered picks of {@code k} out of {@code n}: {@code n! / (n - k)!};
     *         1 if {@code k <= 0}, or {@link Long#MAX_VALUE} if that overflows
     */
    public static long orderedPicks(long n, long k) {
        var picks = 1L;
        for (var factor = n - k + 1; factor <= n && picks != Long.MAX_VALUE; factor++) {
            picks = product(picks, factor);
        }
        return picks;
    }

    /**
     * @param size computes the source's size; any exception it throws means an unknown size
     * @param subject only used for logging
     * @return never negative
     */
    public static long bailOutSize(LongSupplier size, Object subject) {
        long sourceSize;
        try {
            sourceSize = size.getAsLong();
        } catch (RuntimeException ex) {
            // Not only UOE: a custom MoveIteratorFactory with a negative size makes the bridge throw IllegalStateException.
            return unknownSizeBailOut(subject);
        }
        if (sourceSize < 0) {
            LOGGER.trace("        Never-ending iterator or selector ({}) provided a negative size ({}), "
                    + "choosing a bail-out size of ({}) attempts.", subject, sourceSize, UNKNOWN_SIZE_BAIL_OUT);
        }
        return bailOutSize(sourceSize);
    }

    /**
     * For a caller that failed to compute the size itself.
     *
     * @param subject only used for logging
     * @return {@link #UNKNOWN_SIZE_BAIL_OUT}
     */
    public static long unknownSizeBailOut(Object subject) {
        LOGGER.trace(
                "        Never-ending iterator or selector ({}) failed to provide size, choosing a bail-out size of ({}) attempts.",
                subject, UNKNOWN_SIZE_BAIL_OUT);
        return UNKNOWN_SIZE_BAIL_OUT;
    }

    /**
     * @param size the source's size
     * @return 10 times the size, at most {@link #MAX_BAIL_OUT};
     *         {@link #UNKNOWN_SIZE_BAIL_OUT} if the size is negative
     */
    public static long bailOutSize(long size) {
        if (size < 0) {
            return UNKNOWN_SIZE_BAIL_OUT;
        }
        return size > MAX_BAIL_OUT / MULTIPLIER ? MAX_BAIL_OUT : size * MULTIPLIER;
    }

    private SelectionUtils() {
    }

}
