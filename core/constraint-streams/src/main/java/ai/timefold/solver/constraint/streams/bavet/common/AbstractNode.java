package ai.timefold.solver.constraint.streams.bavet.common;

public abstract class AbstractNode {

    private long id;
    private long layerIndex;

    public final void propagateRetracts() {
        PropagationQueue<?> propagationQueue = getPropagationQueue();
        propagationQueue.propagateRetracts();
    }

    public final void propagateUpdates() {
        PropagationQueue<?> propagationQueue = getPropagationQueue();
        propagationQueue.propagateUpdates();
    }

    public final void propagateInserts() {
        PropagationQueue<?> propagationQueue = getPropagationQueue();
        propagationQueue.propagateInserts();
    }

    public final void clearPropagationQueue() {
        PropagationQueue<?> propagationQueue = getPropagationQueue();
        propagationQueue.clear();
    }

    public final void propagateEverything() { // For testing purposes.
        PropagationQueue<?> propagationQueue = getPropagationQueue();
        propagationQueue.propagateRetracts();
        propagationQueue.propagateUpdates();
        propagationQueue.propagateInserts();
        propagationQueue.clear();
    }

    abstract protected PropagationQueue<?> getPropagationQueue();

    public void setId(long id) {
        this.id = id;
    }

    public void setLayerIndex(long id) {
        this.layerIndex = id;
    }

    public long getLayerIndex() {
        return layerIndex;
    }

    @Override
    public String toString() {
        // Useful for debugging if a constraint has multiple nodes of the same type
        return getClass().getSimpleName() + "-" + id + "@" + layerIndex;
    }

}
