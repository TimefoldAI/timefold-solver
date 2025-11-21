package ai.timefold.solver.core.impl.bavet.common.tuple;

public interface OutTupleStorePositionTracker {

    /**
     * Reserves the next position in the output tuple store.
     * Call {@link #computeStoreSize()} to get the final size after all reservations.
     * After that, no further reservations are allowed.
     *
     * @return the reserved position index
     */
    int reserveNextOut();

    /**
     * Finalizes the output store size and prevents further reservations.
     *
     * @return the final output store size
     */
    int computeStoreSize();

}