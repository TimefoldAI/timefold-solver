package ai.timefold.solver.core.impl.bavet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.bavet.common.AbstractRootNode;
import ai.timefold.solver.core.impl.bavet.common.Propagator;
import ai.timefold.solver.core.impl.bavet.common.tuple.ActivitySupport;

import org.jspecify.annotations.NullMarked;

/**
 * Represents Bavet's network of nodes, specific to a particular session.
 */
@NullMarked
public abstract class AbstractBavetNodeNetwork {

    protected static Propagator[][] buildLayeredNodes(List<AbstractNode> nodeList,
            Function<AbstractNode, Propagator> propagatorFunction) {
        var layerMap = new TreeMap<Long, List<Propagator>>();
        nodeList.forEach(node -> layerMap.computeIfAbsent(node.getLayerIndex(), unused -> new ArrayList<>())
                .add(propagatorFunction.apply(node)));
        var layerCount = layerMap.size();
        var layeredNodes = new Propagator[layerCount][];
        for (var i = 0; i < layerCount; i++) {
            var layer = layerMap.get((long) i);
            layeredNodes[i] = layer.toArray(new Propagator[0]);
        }
        return layeredNodes;
    }

    private final Map<Class<?>, List<AbstractRootNode<?>>> declaredClassToNodeMap;

    /**
     * Once {@code activationCheckComplete == true}, only contains nodes which are active.
     * See {@link ActivitySupport#isActive()} for details.
     */
    private Propagator[][] layeredNodes;
    protected boolean activationCheckComplete = false;

    /**
     * @param declaredClassToNodeMap starting nodes, one for each class used in the constraints;
     *        root nodes, layer index 0.
     * @param layeredNodes nodes grouped first by their layer, then by their index within the layer;
     *        propagation needs to happen in this order.
     */
    public AbstractBavetNodeNetwork(Map<Class<?>, List<AbstractRootNode<?>>> declaredClassToNodeMap,
            Propagator[][] layeredNodes) {
        this.declaredClassToNodeMap = declaredClassToNodeMap;
        this.layeredNodes = layeredNodes;
    }

    public int forEachNodeCount() {
        return declaredClassToNodeMap.size();
    }

    public Stream<AbstractRootNode<?>> getRootNodesAcceptingType(Class<?> factClass) {
        // The node needs to match the fact, or the node needs to be applicable to the entire solution.
        // The latter is for FromSolution nodes.
        return declaredClassToNodeMap.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().stream())
                .filter(tupleSourceRoot -> factClass == PlanningSolution.class || tupleSourceRoot.allowsInstancesOf(factClass));
    }

    public void settle() {
        if (activationCheckComplete) { // Simplified loop when the layers were already trimmed.
            for (var layer : layeredNodes) {
                settleLayer(layer);
            }
        } else { // Remove inactive nodes and settle the layers in one go.
            var initializedRootNodes = Collections.newSetFromMap(new IdentityHashMap<>());
            declaredClassToNodeMap.forEach((declaredClass, rootNodes) -> rootNodes.forEach(rootNode -> {
                if (initializedRootNodes.add(rootNode)) {
                    // Ensure one initialization per node.
                    // Root nodes are filled from a session, which can always produce.
                    rootNode.afterAllFactsInserted(true);
                }
            }));

            layeredNodes = Arrays.stream(layeredNodes)
                    .map(layer -> Arrays.stream(layer)
                            .filter(Propagator::isActive)
                            .toArray(Propagator[]::new))
                    .filter(layer -> layer.length > 0)
                    .peek(AbstractBavetNodeNetwork::settleLayer)
                    .toArray(Propagator[][]::new);
            activationCheckComplete = true;
        }
    }

    private static void settleLayer(Propagator[] nodesInLayer) {
        if (nodesInLayer.length == 1) { // Avoid iteration.
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
        if (!(o instanceof AbstractBavetNodeNetwork that))
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
