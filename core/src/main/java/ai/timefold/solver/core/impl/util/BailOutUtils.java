package ai.timefold.solver.core.impl.util;

import java.util.function.LongSupplier;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Computes how many consecutive rejected selections a filtering iterator or selector tries
 * before it gives up on a never-ending source.
 * Trying 10 times the source's size gives reasonable confidence that no acceptable selection was missed.
 * The budget is capped, so that never-ending random iteration always ends in practice.
 */
@NullMarked
public final class BailOutUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(BailOutUtils.class);

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
            LOGGER.trace(
                    "        Never-ending iterator or selector ({}) failed to provide size, choosing a bail-out size of ({}) attempts.",
                    subject, UNKNOWN_SIZE_BAIL_OUT);
            return UNKNOWN_SIZE_BAIL_OUT;
        }
        if (sourceSize < 0) {
            LOGGER.trace("        Never-ending iterator or selector ({}) provided a negative size ({}), "
                    + "choosing a bail-out size of ({}) attempts.", subject, sourceSize, UNKNOWN_SIZE_BAIL_OUT);
        }
        return bailOutSize(sourceSize);
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

    private BailOutUtils() {
    }

}
