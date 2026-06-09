package ai.timefold.solver.core.impl.bavet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;

import ai.timefold.solver.core.impl.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.bavet.common.AbstractRootNode;
import ai.timefold.solver.core.impl.bavet.common.AbstractTwoInputNode;
import ai.timefold.solver.core.impl.bavet.common.Propagator;
import ai.timefold.solver.core.impl.bavet.common.tuple.ActivitySupport;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents Bavet's network of nodes, specific to a particular session.
 */
@NullMarked
public abstract class AbstractBavetNodeNetwork {

    protected static AbstractNode[][] buildLayeredNodes(List<AbstractNode> nodeList) {
        var layerMap = new TreeMap<Long, List<AbstractNode>>();
        nodeList.forEach(node -> layerMap.computeIfAbsent(node.getLayerIndex(), unused -> new ArrayList<>()).add(node));
        var layerCount = layerMap.size();
        var layeredNodes = new AbstractNode[layerCount][];
        for (var i = 0; i < layerCount; i++) {
            var layer = layerMap.get((long) i);
            layeredNodes[i] = layer.toArray(new AbstractNode[0]);
        }
        return layeredNodes;
    }

    private final Map<Class<?>, List<AbstractRootNode<?>>> declaredClassToNodeMap;

    private final AbstractNode[][] layeredNodes;
    private final Function<AbstractNode, Propagator> propagatorFunction;
    /**
     * A subset of {@code layeredNodes}.
     * Once non-null, only contains propagators of nodes which are active.
     * See {@link ActivitySupport#isActive()} for details.
     */
    private Propagator @Nullable [][] layeredActivePropagators;
    /**
     * For testing only: the set of nodes that remained active after {@link #settle()}; null before settle.
     */
    private @Nullable Set<AbstractNode> activeNodeSet;

    /**
     * @param declaredClassToNodeMap starting nodes, one for each class used in the constraints;
     *        root nodes, layer index 0.
     * @param layeredNodes nodes grouped first by their layer, then by their index within the layer;
     *        propagation needs to happen in this order.
     */
    protected AbstractBavetNodeNetwork(Map<Class<?>, List<AbstractRootNode<?>>> declaredClassToNodeMap,
            AbstractNode[][] layeredNodes, Function<AbstractNode, Propagator> propagatorFunction) {
        this.declaredClassToNodeMap = declaredClassToNodeMap;
        this.layeredNodes = layeredNodes;
        this.propagatorFunction = propagatorFunction;
    }

    public int forEachNodeCount() {
        return declaredClassToNodeMap.size();
    }

    /**
     *
     * @param factClass
     * @return if {@link #isActivationCheckComplete()} is true, only returns active root nodes;
     *         otherwise returns all root nodes.
     *         This means that if this information was ever read before activation checks were complete,
     *         it should be re-read after to make sure no inactive nodes are included.
     */
    public Stream<AbstractRootNode<?>> getRootNodesAcceptingType(Class<?> factClass) {
        return declaredClassToNodeMap.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .filter(tupleSourceRoot -> tupleSourceRoot.allowsInstancesOf(factClass))
                .filter(node -> !isActivationCheckComplete() || activeNodeSet.contains(node));
    }

    public void settle() {
        if (layeredActivePropagators == null) {
            // Remove inactive nodes and settle the layers in one go.
            var initializedRootNodes = Collections.newSetFromMap(new IdentityHashMap<>());
            declaredClassToNodeMap.forEach((declaredClass, rootNodes) -> rootNodes.forEach(rootNode -> {
                if (initializedRootNodes.add(rootNode)) {
                    // Ensure one initialization per node.
                    // Root nodes are filled from a session, which can always produce.
                    rootNode.afterAllFactsInserted(true);
                }
            }));

            var activeNodes = Collections.<AbstractNode> newSetFromMap(new IdentityHashMap<>());
            layeredActivePropagators = Arrays.stream(layeredNodes)
                    .map(layer -> Arrays.stream(layer)
                            .filter(s -> switch (s) {
                                case ActivitySupport activityEnabled -> activityEnabled.isActive();
                                case AbstractTwoInputNode<?, ?> twoInputNode -> twoInputNode.isActive();
                            })
                            .peek(activeNodes::add)
                            .map(propagatorFunction).toArray(Propagator[]::new))
                    .filter(layer -> layer.length > 0).peek(AbstractBavetNodeNetwork::settleLayer).toArray(Propagator[][]::new);
            this.activeNodeSet = activeNodes;
            return;
        }
        // Simplified loop when the layers were already trimmed.
        for (var layer : layeredActivePropagators) {
            settleLayer(layer);
        }
    }

    public boolean isActivationCheckComplete() {
        return layeredActivePropagators != null;
    }

    Set<AbstractNode> getActiveNodes() {
        if (activeNodeSet == null) {
            throw new IllegalStateException("Impossible state: getActiveNodes() called before settle().");
        }
        return activeNodeSet;
    }

    /**
     * For testing only. All nodes in the network, regardless of activity.
     */
    List<AbstractNode> getNodes() {
        return Arrays.stream(layeredNodes).flatMap(Arrays::stream).toList();
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
        return "%s with %d forEach nodes.".formatted(getClass().getSimpleName(), forEachNodeCount());
    }

}
