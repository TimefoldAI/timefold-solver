package ai.timefold.solver.core.impl.score.stream.bavet;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.impl.score.stream.bavet.common.Propagator;
import ai.timefold.solver.core.impl.score.stream.bavet.uni.AbstractForEachUniNode;

/**
 * Represents Bavet's network of nodes, specific to a particular session.
 * Nodes only used by disabled constraints have already been removed.
 *
 * @param declaredClassToNodeMap starting nodes, one for each class used in the constraints;
 *        root nodes, layer index 0.
 * @param layeredNodes nodes grouped first by their layer, then by their index within the layer;
 *        propagation needs to happen in this order.
 */
record NodeNetwork(Map<Class<?>, List<AbstractForEachUniNode<Object>>> declaredClassToNodeMap, Propagator[][] layeredNodes) {

    public static final NodeNetwork EMPTY = new NodeNetwork(Map.of(), new Propagator[0][0]);

    public int forEachNodeCount() {
        return declaredClassToNodeMap.size();
    }

    public int layerCount() {
        return layeredNodes.length;
    }

    @SuppressWarnings("unchecked")
    public AbstractForEachUniNode<Object>[] getApplicableForEachNodes(Class<?> factClass) {
        return declaredClassToNodeMap.entrySet()
                .stream()
                .filter(entry -> entry.getKey().isAssignableFrom(factClass))
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .toArray(AbstractForEachUniNode[]::new);
    }

    public void propagate() {
        for (var layerIndex = 0; layerIndex < layerCount(); layerIndex++) {
            propagateInLayer(layeredNodes[layerIndex]);
        }
    }

    private static void propagateInLayer(Propagator[] nodesInLayer) {
        var nodeCount = nodesInLayer.length;
        if (nodeCount == 1) {
            nodesInLayer[0].propagateEverything();
        } else {
            for (var node : nodesInLayer) {
                node.propagateRetracts();
            }
            for (var node : nodesInLayer) {
                node.propagateUpdates();
            }
            for (var node : nodesInLayer) {
                node.propagateInserts();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof NodeNetwork that))
            return false;
        return Objects.equals(declaredClassToNodeMap, that.declaredClassToNodeMap)
                && Objects.deepEquals(layeredNodes, that.layeredNodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(declaredClassToNodeMap, Arrays.deepHashCode(layeredNodes));
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " with " + forEachNodeCount() + " forEach nodes.";
    }

}
