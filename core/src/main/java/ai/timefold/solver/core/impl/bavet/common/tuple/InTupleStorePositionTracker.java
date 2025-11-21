package ai.timefold.solver.core.impl.bavet.common.tuple;

public interface InTupleStorePositionTracker {

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

}