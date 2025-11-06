package ai.timefold.solver.core.impl.bavet.common.tuple;

/**
 * Tracks the size of the output store by allowing reservations of positions.
 * Once the final size is computed, no further reservations can be made
 * and output tuples will be created using this size of their {@link AbstractTuple tuple store}.
 */
public final class OutputStoreSizeTracker implements TupleStorePositionTracker {

    private int effectiveOutputStoreSize;
    private int finalOutputStoreSize = -1;

    public OutputStoreSizeTracker(int initialSize) {
        if (initialSize < 0) {
            throw new IllegalArgumentException(
                    "Impossible state: The initialSize (%d) must be non-negative.".formatted(initialSize));
        }
        this.effectiveOutputStoreSize = initialSize;
    }

    /**
     * @return the next available position in the output store, reserved exclusively for use by the caller
     * @throws IllegalStateException if {@link #computeOutputStoreSize()} has already been called.
     */
    @Override
    public int reserveNextAvailablePosition() {
        if (finalOutputStoreSize >= 0) {
            throw new IllegalStateException("Impossible state: The finalOutputStoreSize (%s) has already been computed."
                    .formatted(finalOutputStoreSize));
        }
        return effectiveOutputStoreSize++;
    }

    /**
     * Finalizes the output store size and prevents further reservations.
     * 
     * @return the final output store size
     */
    public int computeOutputStoreSize() {
        if (finalOutputStoreSize < 0) {
            finalOutputStoreSize = effectiveOutputStoreSize;
        }
        return finalOutputStoreSize;
    }

}
