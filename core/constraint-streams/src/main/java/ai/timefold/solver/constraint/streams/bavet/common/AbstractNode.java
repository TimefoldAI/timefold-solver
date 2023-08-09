package ai.timefold.solver.constraint.streams.bavet.common;

public abstract class AbstractNode {

    private long id;
    private long layerIndex;

    public final void calculateScore() {
        getPropagationQueue().propagateAndClear();
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
