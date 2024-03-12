package ai.timefold.solver.core.impl.score.stream.bavet.common;

import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintSession;

/**
 * Propagator is an interface that exposes the {@link PropagationQueue} to {@link BavetConstraintSession}.
 * It does not include the methods for inserting/updating/retracting facts,
 * as the session accesses them via the {@link AbstractNode}.
 * <p>
 * Nodes come in layers, and each layer has {@link AbstractNode#getLayerIndex() a unique index}.
 * Nodes in higher layers receive their inputs from nodes in lower layers.
 * Propagation starts from the lowest layer ({@code 0}) and goes up.
 * Layer N+1 only starts propagating after layer N has completed its propagation,
 * that is after {@link #propagateRetracts()},
 * {@link #propagateUpdates()} and {@link #propagateInserts()} have been called on every node in the layer.
 * This happens in and is guaranteed by {@link BavetConstraintSession#calculateScore(int)}.
 * <p>
 * Nodes in a layer do not propagate entirely independently.
 * In fact, we first call {@link #propagateRetracts()} on all nodes in the layer,
 * then {@link #propagateUpdates()} and finally {@link #propagateInserts()}.
 * This order of operations is necessary to ensure consistent data in the higher layer nodes.
 * <p>
 * Example: consider a join node that joins two different classes of facts, say A and B.
 * The join node has two inputs, one for each type of fact.
 * Both of these inputs are in the same layer, say layer 1.
 * This puts the join node in layer 2.
 * If we had the left input propagate updates and inserts before the right input propagates retracts,
 * a situation could occur where the left input is attempting to join with something that will later be retracted.
 * And this, in turn, could result in a runtime exception,
 * because the right input data at that point is stale.
 * By running retracts on all nodes in a layer first, and then all updates and then all inserts,
 * we make sure that the join node always has up-to-date data.
 * And due to the fact that propagation is done in layers,
 * the join propagations will only ever be triggered after its inputs have completed their propagation.
 * <p>
 * As this is critical to the correctness of Bavet,
 * there is specific test coverage for these corner cases.
 *
 * @see PropagationQueue More information about propagation.
 */
public sealed interface Propagator permits PropagationQueue {

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

    /**
     * Convenience method for cases where the node layer only contains a single node,
     * and therefore it can be propagated all at once.
     */
    default void propagateEverything() {
        propagateRetracts();
        propagateUpdates();
        propagateInserts();
    }

}
