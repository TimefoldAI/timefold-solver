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
 * After all items in the queue are propagated, the queue is cleared.
 *
 * @param <T>
 */
public sealed interface PropagationQueue<T> permits AbstractDynamicPropagationQueue, StaticPropagationQueue {

    void insert(T item);

    void update(T item);

    void retract(T item, TupleState state);

    void propagateRetracts();

    void propagateUpdates();

    void propagateInserts();

    void clear();

}
