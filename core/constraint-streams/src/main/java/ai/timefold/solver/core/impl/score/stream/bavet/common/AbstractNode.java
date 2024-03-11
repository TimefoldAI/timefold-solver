package ai.timefold.solver.core.impl.score.stream.bavet.common;

import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintSession;

/**
 * @see PropagationQueue Description of the propagation mechanism.
 */
public abstract class AbstractNode {

    private long id;
    private long layerIndex = -1;

    /**
     * Instead of calling the propagation directly from here,
     * we export the propagation queue and allow {@link BavetConstraintSession} to call it.
     * This is done with the idea that {@link Propagator} only has two implementations
     * (unlike {@link AbstractNode} with myriad implementations)
     * and therefore JVM call site optimizations will kick in to make the method dispatch faster.
     *
     * @return never null; the {@link PropagationQueue} in use by this node
     */
    public abstract Propagator getPropagator();

    public final void setId(long id) {
        this.id = id;
    }

    public final void setLayerIndex(long layerIndex) {
        if (layerIndex < 0) {
            throw new IllegalArgumentException("Impossible state: layer index (" + layerIndex + ") must be at least 0.");
        }
        this.layerIndex = layerIndex;
    }

    public final long getLayerIndex() {
        if (layerIndex == -1) {
            throw new IllegalStateException(
                    "Impossible state: layer index for node (" + this + ") requested before being set.");
        }
        return layerIndex;
    }

    @Override
    public String toString() {
        // Useful for debugging if a constraint has multiple nodes of the same type
        return getClass().getSimpleName() + "-" + id + "@" + layerIndex;
    }

}
