package ai.timefold.solver.core.impl.bavet.common.tuple;

public interface TupleStoreSizeTracker {

    /**
     * Finalizes the output store size and prevents further reservations.
     *
     * @return the final output store size
     */
    int computeStoreSize();

}