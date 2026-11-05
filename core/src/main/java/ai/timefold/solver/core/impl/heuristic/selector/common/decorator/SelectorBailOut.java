package ai.timefold.solver.core.impl.heuristic.selector.common.decorator;

import java.util.function.LongSupplier;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Computes how many consecutive rejected selections a filtering selector tries
 * before it gives up on a never-ending child.
 * Trying 10 times the child's size gives reasonable confidence that no acceptable selection was missed.
 */
@NullMarked
public final class SelectorBailOut {

    private static final Logger LOGGER = LoggerFactory.getLogger(SelectorBailOut.class);

    public static final long MULTIPLIER = 10L;
    /**
     * Used when the child's size is unknown, negative, or too large to multiply.
     */
    public static final long UNKNOWN_SIZE_BAIL_OUT = Short.MAX_VALUE * MULTIPLIER;

    /**
     * @param childSize computes the child's size; may throw {@link UnsupportedOperationException}
     * @param childSelector only used for logging
     * @return never negative
     */
    public static long of(LongSupplier childSize, Object childSelector) {
        long size;
        try {
            size = childSize.getAsLong();
        } catch (UnsupportedOperationException ex) {
            // Only an unknown size is tolerated here; any other exception signals a genuine defect and must propagate.
            LOGGER.trace(
                    "        Never-ending selector ({}) failed to provide size, choosing a bail-out size of ({}) attempts.",
                    childSelector, UNKNOWN_SIZE_BAIL_OUT);
            return UNKNOWN_SIZE_BAIL_OUT;
        }
        if (size < 0 || size > Long.MAX_VALUE / MULTIPLIER) {
            LOGGER.trace("        Never-ending selector ({}) provided an unusable size ({}), "
                    + "choosing a bail-out size of ({}) attempts.", childSelector, size, UNKNOWN_SIZE_BAIL_OUT);
            return UNKNOWN_SIZE_BAIL_OUT;
        }
        return size * MULTIPLIER;
    }

    private SelectorBailOut() {
    }

}
