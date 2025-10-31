package ai.timefold.solver.core.impl.bavet;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.bavet.common.BavetRootNode;
import ai.timefold.solver.core.impl.bavet.common.ConstraintProfiler;
import ai.timefold.solver.core.impl.bavet.common.Propagator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents Bavet's network of nodes, specific to a particular session.
 * Nodes only used by disabled constraints have already been removed.
 *
 * @param declaredClassToNodeMap starting nodes, one for each class used in the constraints;
 *        root nodes, layer index 0.
 * @param layeredNodes nodes grouped first by their layer, then by their index within the layer;
 *        propagation needs to happen in this order.
 */
@NullMarked
public record NodeNetwork(Map<Class<?>, List<BavetRootNode<?>>> declaredClassToNodeMap,
        Propagator[][] layeredNodes, @Nullable ConstraintProfiler constraintProfiler) {

    public static final NodeNetwork EMPTY = new NodeNetwork(Map.of(), new Propagator[0][0], null);

    public int forEachNodeCount() {
        return declaredClassToNodeMap.size();
    }

    public int layerCount() {
        return layeredNodes.length;
    }

    public Stream<BavetRootNode<?>> getRootNodes() {
        return declaredClassToNodeMap.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().stream());
    }

    public Stream<BavetRootNode<?>> getRootNodesAcceptingType(Class<?> factClass) {
        // The node needs to match the fact, or the node needs to be applicable to the entire solution.
        // The latter is for FromSolution nodes.
        return declaredClassToNodeMap.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().stream())
                .filter(tupleSourceRoot -> factClass == PlanningSolution.class || tupleSourceRoot.allowsInstancesOf(factClass));
    }

    public void settle() {
        for (var layerIndex = 0; layerIndex < layerCount(); layerIndex++) {
            settleLayer(layeredNodes[layerIndex]);
        }
    }

    private static void settleLayer(Propagator[] nodesInLayer) {
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

    public void summarizeProfileIfPresent() {
        if (constraintProfiler != null) {
            constraintProfiler.summarize();
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
        return "%s with %d forEach nodes."
                .formatted(getClass().getSimpleName(), forEachNodeCount());
    }

}
