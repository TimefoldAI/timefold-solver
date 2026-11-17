package ai.timefold.solver.core.impl.util;

import java.util.function.IntToLongFunction;
import java.util.function.ToLongBiFunction;
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

    public static final long BAIL_OUT_MULTIPLIER = 10L;
    /**
     * Used when the source's size is unknown or negative.
     */
    public static final long UNKNOWN_SIZE_BAIL_OUT = Short.MAX_VALUE * BAIL_OUT_MULTIPLIER;
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
     *         1 if {@code k <= 0}, 0 if {@code k > n},
     *         or {@link Long#MAX_VALUE} if that overflows
     */
    public static long orderedPicks(long n, long k) {
        if (k > n) {
            return 0L;
        }
        var picks = 1L;
        for (var factor = n - k + 1; factor <= n && picks != Long.MAX_VALUE; factor++) {
            picks = product(picks, factor);
        }
        return picks;
    }

    /**
     * @param count how many there are to pick from
     * @return the sum of {@link #orderedPicks(long, long)} for {@code minimumPicks <= k <= maximumPicks};
     *         picks above the count add nothing
     */
    public static long orderedPicksSum(long count, int minimumPicks, int maximumPicks) {
        var lastPicks = (int) Math.min(maximumPicks, count);
        return sumOf(minimumPicks, lastPicks, picks -> orderedPicks(count, picks));
    }

    /**
     * With a non-capturing method reference, such as {@code MoveSelector::getSize}, nothing is allocated per call.
     *
     * @param source the selector whose size is computed; also used for logging
     * @param size any exception it throws means an unknown size
     * @return never negative
     */
    public static <Source_> long bailOutSize(Source_ source, ToLongFunction<Source_> size) {
        long sourceSize;
        try {
            sourceSize = size.applyAsLong(source);
        } catch (RuntimeException ex) {
            return unknownSizeBailOut(source);
        }
        return bailOutSize(sourceSize, source);
    }

    /**
     * As {@link #bailOutSize(Object, ToLongFunction)}, for a size that depends on an argument.
     * With a non-capturing method reference, such as {@code ValueSelector::getSize}, nothing is allocated per call.
     *
     * @param source the selector whose size is computed; also used for logging
     * @param size any exception it throws means an unknown size
     * @return never negative
     */
    public static <Source_, Argument_> long bailOutSize(Source_ source, Argument_ argument,
            ToLongBiFunction<Source_, Argument_> size) {
        long sourceSize;
        try {
            sourceSize = size.applyAsLong(source, argument);
        } catch (RuntimeException ex) {
            return unknownSizeBailOut(source);
        }
        return bailOutSize(sourceSize, source);
    }

    private static long unknownSizeBailOut(Object subject) {
        // Not only UOE: a custom MoveIteratorFactory with a negative size makes the bridge throw IllegalStateException.
        LOGGER.trace(
                "        Never-ending iterator or selector ({}) failed to provide size, choosing a bail-out size of ({}) attempts.",
                subject, UNKNOWN_SIZE_BAIL_OUT);
        return UNKNOWN_SIZE_BAIL_OUT;
    }

    private static long bailOutSize(long size, Object subject) {
        if (size < 0) {
            LOGGER.trace("        Never-ending iterator or selector ({}) provided a negative size ({}), "
                    + "choosing a bail-out size of ({}) attempts.", subject, size, UNKNOWN_SIZE_BAIL_OUT);
        }
        return bailOutSize(size);
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
        return size > MAX_BAIL_OUT / BAIL_OUT_MULTIPLIER ? MAX_BAIL_OUT : size * BAIL_OUT_MULTIPLIER;
    }

    private SelectionUtils() {
    }

}
