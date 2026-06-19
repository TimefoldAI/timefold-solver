package ai.timefold.solver.core.impl.neighborhood;

import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.impl.bavet.AbstractBavetNodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.bavet.common.AbstractRootNode;

import org.jspecify.annotations.NullMarked;

/**
 * Represents Neighborhoods' network of nodes, specific to a particular session.
 */
@NullMarked
public final class NeighborhoodsBavetNodeNetwork extends AbstractBavetNodeNetwork {

    public static NeighborhoodsBavetNodeNetwork of(List<AbstractNode> nodeList,
            Map<Class<?>, List<AbstractRootNode<?>>> declaredClassToNodeMap) {
        var layeredNodes = AbstractBavetNodeNetwork.buildLayeredNodes(nodeList);
        return new NeighborhoodsBavetNodeNetwork(declaredClassToNodeMap, layeredNodes);
    }

    /**
     * @param declaredClassToNodeMap starting nodes, one for each class used in the constraints;
     *        root nodes, layer index 0.
     * @param layeredNodes nodes grouped first by their layer, then by their index within the layer;
     *        propagation needs to happen in this order.
     */
    private NeighborhoodsBavetNodeNetwork(Map<Class<?>, List<AbstractRootNode<?>>> declaredClassToNodeMap,
            AbstractNode[][] layeredNodes) {
        super(declaredClassToNodeMap, layeredNodes, AbstractNode::getPropagator);
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
