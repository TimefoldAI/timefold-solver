package ai.timefold.solver.core.impl.bavet.common.tuple;

public interface TupleStorePositionTracker extends TupleStoreSizeTracker {

    /**
     * Reserves the next position in the left tuple store.
     *
     * @return the reserved position index
     */
    int reserveNextLeft();

    /**
     * Reserves the next position in the right tuple store.
     *
     * @return the reserved position index
     */
    int reserveNextRight();

    /**
     * Reserves the next position in the output tuple store.
     * Call {@link #computeStoreSize()} to get the final size after all reservations.
     * After that, no further reservations are allowed.
     *
     * @return the reserved position index
     */
    int reserveNextOut();

}