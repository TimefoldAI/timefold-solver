package ai.timefold.solver.core.impl.neighborhood;

import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.impl.bavet.AbstractBavetNodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.AbstractRootNode;
import ai.timefold.solver.core.impl.bavet.common.Propagator;

import org.jspecify.annotations.NullMarked;

/**
 * Represents Neighborhoods' network of nodes, specific to a particular session.
 */
@NullMarked
public final class NeighborhoodsBavetNodeNetwork extends AbstractBavetNodeNetwork {

    /**
     * @param declaredClassToNodeMap starting nodes, one for each class used in the constraints;
     *        root nodes, layer index 0.
     * @param layeredNodes nodes grouped first by their layer, then by their index within the layer;
     *        propagation needs to happen in this order.
     */
    public NeighborhoodsBavetNodeNetwork(Map<Class<?>, List<AbstractRootNode<?>>> declaredClassToNodeMap,
            Propagator[][] layeredNodes) {
        super(declaredClassToNodeMap, layeredNodes);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NeighborhoodsBavetNodeNetwork))
            return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
