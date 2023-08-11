package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

/**
 * A propagation queue is an ordered collection of items that need to be sent to the next node.
 * This operation is called propagation.
 * <p>
 * In order to properly update joins, removing stale data before joining is required.
 * To that end, the queue propagates retracts first, then updates and finally inserts.
 * Other than that, the order of propagation is the order of insertion into the queue.
 * <p>
 * Once an item propagates, it is set to a stable state that indicates it has been propagated,
 * either {@link TupleState#OK} or {@link TupleState#DEAD}.
 * Subsequent nodes will only be able to see the state of the tuple as it is being propagated to them.
 * During their own propagation cycles, which happen after the original propagation cycle is complete,
 * they will only see one of the two stable states.
 * <p>
 * The propagation operation consists of three phases:
 * <ol>
 * <li>Retracts: {@link #propagateRetracts()}</li>
 * <li>Updates: {@link #propagateUpdates()}</li>
 * <li>Inserts: {@link #propagateInserts()}</li>
 * </ol>
 * All of them need to be called, and in this order.
 * Otherwise, the queue will be in an inconsistent state,
 * likely resulting in runtime exceptions and/or score corruption.
 * <p>
 * The reason why these operations are not combined into a single method is the fact
 * that multiple nodes may need to execute their retracts first,
 * and only when all of those are propagated, the rest of the phases can start.
 *
 * @see AbstractNode More information about propagation.
 * @param <T>
 */
public sealed interface PropagationQueue<T> permits DynamicPropagationQueue, StaticPropagationQueue {

    void insert(T item);

    void update(T item);

    void retract(T item, TupleState state);

    /**
     * Starts the propagation event. Must be followed by {@link #propagateUpdates()}.
     */
    void propagateRetracts();

    /**
     * Must be preceded by {@link #propagateRetracts()} and followed by {@link #propagateInserts()}.
     */
    void propagateUpdates();

    /**
     * Must by preceded by {@link #propagateRetracts()} and {@link #propagateUpdates()}.
     * Ends the propagation event and clears the queue.
     */
    void propagateInserts();

}
