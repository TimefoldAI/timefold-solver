package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintSession;

/**
 * Nodes come in layers, and each layer has {@link #getLayerIndex() a unique index}.
 * Nodes in higher layers have inputs from nodes in lower layers, they depend on them.
 * Propagation starts from the lowest layer ({@code 0}) and goes up.
 * Layer N+1 only starts propagating after layer N has completed its propagation,
 * that is after {@link #propagateRetracts()},
 * {@link #propagateUpdates()} and {@link #propagateInserts()} have been called on every node in the layer.
 * This happens in and is guaranteed by {@link BavetConstraintSession#calculateScore(int)}.
 * <p>
 * Nodes in a layer do not propagate entirely independently.
 * In fact, we first call {@link #propagateRetracts()} on all nodes in the layer,
 * then {@link #propagateUpdates()} and finally {@link #propagateInserts()}.
 * This is because nodes in higher layers depend on nodes in lower layers,
 * and this order of operations is necessary to ensure consistent data in the higher layer nodes.
 * <p>
 * Example: consider a join node that joins two different classes of facts, say A and B.
 * The join node has two inputs, one for each type of fact.
 * Both of these inputs are in the same layer, say layer 1.
 * This puts the join node in layer 2.
 * If we had the left input propagate updates and inserts before the right input propagates retracts,
 * a situation could occur where the left input is attempting to join with something that will later be retracted.
 * And this, in turn, would result in a runtime exception,
 * because the right input data at that point is stale.
 * By running retracts on all nodes in a layer first, and then all updates and then all inserts,
 * we make sure that the join node always has up-to-date data.
 * And due to the fact that propagation is done in layers,
 * the join propagations will only ever be triggered after its inputs have completed their propagation.
 * <p>
 * As this is a critical to the correctness of Bavet,
 * there is specific test coverage for these corner cases.
 *
 * @see PropagationQueue More information about propagation.
 */
public abstract class AbstractNode {

    private long id;
    private long layerIndex;

    public abstract void propagateRetracts();

    public abstract void propagateUpdates();

    public abstract void propagateInserts();

    public final void propagateEverything() {
        propagateRetracts();
        propagateUpdates();
        propagateInserts();
    }

    public final void setId(long id) {
        this.id = id;
    }

    public final void setLayerIndex(long id) {
        this.layerIndex = id;
    }

    public final long getLayerIndex() {
        return layerIndex;
    }

    @Override
    public String toString() {
        // Useful for debugging if a constraint has multiple nodes of the same type
        return getClass().getSimpleName() + "-" + id + "@" + layerIndex;
    }

}
