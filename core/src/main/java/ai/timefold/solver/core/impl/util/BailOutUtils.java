package ai.timefold.solver.core.impl.util;

import java.util.function.LongSupplier;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Computes how many consecutive rejected selections a filtering iterator or selector tries
 * before it gives up on a never-ending source.
 * Trying 10 times the source's size gives reasonable confidence that no acceptable selection was missed.
 */
@NullMarked
public final class BailOutUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(BailOutUtils.class);

    public static final long MULTIPLIER = 10L;
    /**
     * Used when the source's size is unknown, negative, or too large to multiply.
     */
    public static final long UNKNOWN_SIZE_BAIL_OUT = Short.MAX_VALUE * MULTIPLIER;

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
        if (!isUsable(sourceSize)) {
            LOGGER.trace("        Never-ending iterator or selector ({}) provided an unusable size ({}), "
                    + "choosing a bail-out size of ({}) attempts.", subject, sourceSize, UNKNOWN_SIZE_BAIL_OUT);
        }
        return bailOutSize(sourceSize);
    }

    /**
     * @param size the source's size
     * @return 10 times the size, or {@link #UNKNOWN_SIZE_BAIL_OUT} if the size is negative or too large; never negative
     */
    public static long bailOutSize(long size) {
        return isUsable(size) ? size * MULTIPLIER : UNKNOWN_SIZE_BAIL_OUT;
    }

    private static boolean isUsable(long size) {
        return size >= 0 && size <= Long.MAX_VALUE / MULTIPLIER;
    }

    private BailOutUtils() {
    }

}
