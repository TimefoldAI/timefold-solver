package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NonNull;

public final class VariableReferenceGraphBuilder<Solution_> {

    final ChangedVariableNotifier<Solution_> changedVariableNotifier;
    final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>>> variableReferenceToBeforeProcessor;
    final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>>> variableReferenceToAfterProcessor;
    final List<GraphNode<Solution_>> nodeList;
    final Map<Object, Integer> entityToEntityId;
    final Map<GraphNode<Solution_>, List<GraphNode<Solution_>>> fixedEdges;
    final Map<GraphNode<Solution_>, List<GraphNode<Solution_>>> initialDynamicEdges;
    final Map<VariableMetaModel<?, ?, ?>, Map<Object, GraphNode<Solution_>>> variableReferenceToContainingNodeMap;
    final Map<Integer, Map<Object, GraphNode<Solution_>>> variableGroupIdToContainingNodeMap;
    final Map<VariableMetaModel<?, ?, ?>, List<ListElementSourceLocator>> listVariableReferenceToElementLocator;
    boolean isGraphFixed;

    public VariableReferenceGraphBuilder(ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        this.changedVariableNotifier = changedVariableNotifier;
        nodeList = new ArrayList<>();
        variableReferenceToContainingNodeMap = new HashMap<>();
        variableGroupIdToContainingNodeMap = new HashMap<>();
        variableReferenceToBeforeProcessor = new HashMap<>();
        variableReferenceToAfterProcessor = new HashMap<>();
        fixedEdges = new HashMap<>();
        initialDynamicEdges = new HashMap<>();
        entityToEntityId = new IdentityHashMap<>();
        listVariableReferenceToElementLocator = new HashMap<>();
        isGraphFixed = true;
    }

    /**
     * Adds an edge that reflects the state of a planning list variable when the graph is built.
     * Unlike a {@link #addFixedEdge(GraphNode, GraphNode) fixed edge}, it may be removed during solving
     * when the list variable changes, so a dependency loop through it can be broken by the solver
     * and must not fail fast.
     */
    public void addInitialDynamicEdge(@NonNull GraphNode<Solution_> from, @NonNull GraphNode<Solution_> to) {
        if (from.graphNodeId() == to.graphNodeId()) {
            return;
        }
        initialDynamicEdges.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    }

    public void addListElementSourceLocator(VariableMetaModel<?, ?, ?> listVariableId,
            ListElementSourceLocator listElementSourceLocator) {
        isGraphFixed = false;
        listVariableReferenceToElementLocator.computeIfAbsent(listVariableId, k -> new ArrayList<>())
                .add(listElementSourceLocator);
    }

    public <Entity_> void addVariableReferenceEntity(Entity_ entity, boolean hasDynamicEdges,
            VariableUpdaterInfo<Solution_> variableUpdater) {
        var groupId = variableUpdater.groupId();
        var isGroup = variableUpdater.groupEntities() != null;
        var entityRepresentative = entity;
        if (isGroup) {
            entityRepresentative = (Entity_) variableUpdater.groupEntities()[0];
        }
        var instanceMap = variableGroupIdToContainingNodeMap.get(groupId);

        var instance = instanceMap == null ? null : instanceMap.get(entityRepresentative);
        if (instance != null) {
            return;
        }
        if (instanceMap == null) {
            instanceMap = new IdentityHashMap<>();
            variableGroupIdToContainingNodeMap.put(groupId, instanceMap);
        }

        var entityId = entityToEntityId.computeIfAbsent(entityRepresentative, ignored -> entityToEntityId.size());
        int[] groupEntityIds = null;

        if (isGroup) {
            var groupEntities = variableUpdater.groupEntities();
            groupEntityIds = new int[groupEntities.length];
            for (var i = 0; i < groupEntityIds.length; i++) {
                var groupEntity = variableUpdater.groupEntities()[i];
                groupEntityIds[i] = entityToEntityId.computeIfAbsent(groupEntity, ignored -> entityToEntityId.size());
            }
        }

        var node = new GraphNode<>(entityRepresentative, Collections.singletonList(variableUpdater), nodeList.size(),
                entityId, groupEntityIds, hasDynamicEdges);

        if (isGroup) {
            for (var groupEntity : variableUpdater.groupEntities()) {
                addToInstanceMaps(instanceMap, groupEntity, node, variableUpdater);
            }
        } else {
            addToInstanceMaps(instanceMap, entity, node, variableUpdater);
        }
        nodeList.add(node);
    }

    private void addToInstanceMaps(Map<Object, GraphNode<Solution_>> instanceMap,
            Object entity, GraphNode<Solution_> node, VariableUpdaterInfo<Solution_> variableUpdater) {
        instanceMap.put(entity, node);
        var variableInstanceMap =
                variableReferenceToContainingNodeMap.computeIfAbsent(variableUpdater.id(), ignored -> new IdentityHashMap<>());
        variableInstanceMap.put(entity, node);
    }

    public void addFixedEdge(@NonNull GraphNode<Solution_> from, @NonNull GraphNode<Solution_> to) {
        if (from.graphNodeId() == to.graphNodeId()) {
            return;
        }
        fixedEdges.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    }

    public void addBeforeProcessor(GraphChangeType graphChangeType, VariableMetaModel<?, ?, ?> variableId,
            BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object> consumer) {
        isGraphFixed &= !graphChangeType.affectsGraph();
        variableReferenceToBeforeProcessor.computeIfAbsent(variableId, k -> new ArrayList<>())
                .add(consumer);
    }

    public void addAfterProcessor(GraphChangeType graphChangeType, VariableMetaModel<?, ?, ?> variableId,
            BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object> consumer) {
        isGraphFixed &= !graphChangeType.affectsGraph();
        variableReferenceToAfterProcessor.computeIfAbsent(variableId, k -> new ArrayList<>())
                .add(consumer);
    }

    public VariableReferenceGraph build(IntFunction<TopologicalOrderGraph> graphCreator,
            boolean ignoreInconsistentSolutions) {
        assertNoFixedLoops();
        if (nodeList.isEmpty()) {
            return EmptyVariableReferenceGraph.INSTANCE;
        }
        mergeSameEntityNodes();
        if (isGraphFixed) {
            return new FixedVariableReferenceGraph<>(this, graphCreator);
        }
        return new DefaultVariableReferenceGraph<>(this, graphCreator, ignoreInconsistentSolutions);
    }

    public @NonNull GraphNode<Solution_> lookupOrError(VariableMetaModel<?, ?, ?> variableId, Object entity) {
        var out = variableReferenceToContainingNodeMap.getOrDefault(variableId, Collections.emptyMap()).get(entity);
        if (out == null) {
            throw new IllegalArgumentException();
        }
        return out;
    }

    /**
     * Merges the nodes of each entity that are connected by a direct fixed edge
     * into a single node, when the merge does not create a loop.
     * A merged node's variableReferences are in the topological order of the merged subgraph.
     */
    private void mergeSameEntityNodes() {
        var size = nodeList.size();
        if (size < 2) {
            return;
        }
        // The topological order of the graph before the merge;
        // assertNoFixedLoops() guarantees the fixed edges are acyclic.
        var orderGraph = new DefaultTopologicalOrderGraph(size);
        var forwardEdges = new BitSet[size];
        var backwardEdges = new BitSet[size];
        for (var i = 0; i < size; i++) {
            forwardEdges[i] = new BitSet();
            backwardEdges[i] = new BitSet();
        }
        for (var edgeEntry : this.fixedEdges.entrySet()) {
            addEdgeToAdjacency(orderGraph, forwardEdges, backwardEdges, edgeEntry.getKey().graphNodeId(),
                    edgeEntry.getValue());
        }
        orderGraph.commitChanges(new BitSet());

        // Find the connected components of the same-entity nodes via the direct edges;
        // only those nodes can be merged into one node.
        var entityToNodeIds = new IdentityHashMap<Object, BitSet>();
        for (var i = 0; i < size; i++) {
            entityToNodeIds.computeIfAbsent(nodeList.get(i).entity(), ignored -> new BitSet()).set(i);
        }

        // component = same entity nodes connected by fixed edges. Nodes with non-empty groups are not considered
        // part of the component.
        var componentIndexOfNode = new int[size];
        Arrays.fill(componentIndexOfNode, -1); // -1 = no connected same entity nodes
        var componentToNodeIds = new ArrayList<BitSet>();
        var visited = new boolean[size];
        for (var nodeIds : entityToNodeIds.values()) {
            for (var startId : nodeIds.stream().toArray()) {
                if (visited[startId] || nodeList.get(startId).groupEntityIds() != null) {
                    continue;
                }
                var component = new BitSet();
                var queue = new ArrayDeque<>(List.of(startId));
                visited[startId] = true;

                // Do a DFS in both the forward and backward direction, since the node might not be
                // the first one in a chain
                while (!queue.isEmpty()) {
                    var currentId = queue.poll();
                    component.set(currentId);
                    for (var nextId : forwardEdges[currentId].stream().toArray()) {
                        if (!visited[nextId] && isSameEntityNode(nextId, startId, backwardEdges)) {
                            visited[nextId] = true;
                            queue.add(nextId);
                        }
                    }
                    for (var nextId : backwardEdges[currentId].stream().toArray()) {
                        if (!visited[nextId] && isSameEntityNode(nextId, startId, backwardEdges)) {
                            visited[nextId] = true;
                            queue.add(nextId);
                        }
                    }
                }
                if (component.size() > 1) {
                    var componentIndex = componentToNodeIds.size();
                    for (var nodeId : component.stream().toArray()) {
                        componentIndexOfNode[nodeId] = componentIndex;
                    }
                    componentToNodeIds.add(component);
                }
            }
        }

        var canMergeComponent = canMergeComponents(componentToNodeIds, componentIndexOfNode, size);
        var anyCanMerge = false;
        for (var canMerge : canMergeComponent) {
            anyCanMerge |= canMerge;
        }
        if (anyCanMerge) {
            replaceNodesWithMergedNodes(componentToNodeIds, canMergeComponent, componentIndexOfNode, orderGraph);
        }
    }

    private boolean isSameEntityNode(int nodeId, int otherId, BitSet[] backwardEdges) {
        var node = nodeList.get(nodeId);
        if (!(node.groupEntityIds() == null && node.entity() == nodeList.get(otherId).entity())) {
            return false;
        }
        return countAncestorsWithDynamicEdges(nodeId, backwardEdges) == countAncestorsWithDynamicEdges(otherId, backwardEdges);
    }

    private int countAncestorsWithDynamicEdges(int nodeId, BitSet[] backwardEdges) {
        var visited = new BitSet();
        var queue = new ArrayDeque<Integer>();
        queue.add(nodeId);
        visited.set(nodeId);
        var count = 0;
        while (!queue.isEmpty()) {
            var next = queue.poll();
            if (nodeList.get(next).hasDynamicEdges()) {
                count++;
            }
            for (var nextId : backwardEdges[next].stream().toArray()) {
                if (!visited.get(nextId)) {
                    visited.set(nextId);
                    queue.add(nextId);
                }
            }
        }
        return count;
    }

    /**
     * @return for each component, whether merging it together with all the other mergeable
     *         components does not create a loop
     */
    private boolean[] canMergeComponents(List<BitSet> componentToNodeIds, int[] componentIndexOfNode,
            int graphNodeCount) {
        // A vertex for every component and for every node that is not in a component.
        var componentCount = componentToNodeIds.size();

        // Maps a node in the original graph to a node in the contracted graph
        var nodeToVertex = new int[graphNodeCount];

        // Nodes in the contracted graph
        var vertexCount = componentCount;
        for (var i = 0; i < graphNodeCount; i++) {
            if (componentIndexOfNode[i] >= 0) {
                nodeToVertex[i] = componentIndexOfNode[i];
            } else {
                nodeToVertex[i] = vertexCount++;
            }
        }
        var contractedGraph = new DefaultTopologicalOrderGraph(vertexCount);

        // Map fixed edges in the original graph to edges in the contracted graph
        for (var edgeEntry : this.fixedEdges.entrySet()) {
            var fromVertex = nodeToVertex[edgeEntry.getKey().graphNodeId()];
            for (var toNode : edgeEntry.getValue()) {
                var toVertex = nodeToVertex[toNode.graphNodeId()];
                if (fromVertex != toVertex) {
                    contractedGraph.addEdge(fromVertex, toVertex);
                }
            }
        }
        contractedGraph.commitChanges(new BitSet());
        var out = new boolean[componentCount];
        for (var i = 0; i < componentCount; i++) {
            // A component that is on a loop in the contracted graph would
            // create the loop when it and the other mergeable components are merged.
            out[i] = contractedGraph.getComponent(i).size() == 1;
        }
        return out;
    }

    private static <Solution_> void addEdgeToAdjacency(DefaultTopologicalOrderGraph orderGraph, BitSet[] forwardEdges,
            BitSet[] backwardEdges, int fromNodeId, List<GraphNode<Solution_>> toNodes) {
        for (var toNode : toNodes) {
            var toNodeId = toNode.graphNodeId();
            orderGraph.addEdge(fromNodeId, toNodeId);
            forwardEdges[fromNodeId].set(toNodeId);
            backwardEdges[toNodeId].set(fromNodeId);
        }
    }

    private void replaceNodesWithMergedNodes(List<BitSet> componentToNodeIds,
            boolean[] canMergeComponent, int[] componentIndexOfNode, DefaultTopologicalOrderGraph orderGraph) {
        var oldNodeIdToNewNodeId = new int[nodeList.size()];
        var mergedNodeIdOfComponent = new int[componentToNodeIds.size()];
        Arrays.fill(mergedNodeIdOfComponent, -1);
        var newNodeList = new ArrayList<GraphNode<Solution_>>(nodeList.size());
        for (var oldNodeId = 0; oldNodeId < nodeList.size(); oldNodeId++) {
            var oldNode = nodeList.get(oldNodeId);
            var componentIndex = componentIndexOfNode[oldNodeId];
            var isMerged = componentIndex >= 0 && canMergeComponent[componentIndex];
            if (isMerged) {
                // Only create a new node for the component if we haven't done so already
                if (mergedNodeIdOfComponent[componentIndex] == -1) {
                    mergedNodeIdOfComponent[componentIndex] = newNodeList.size();
                    newNodeList.add(createMergedNode(componentToNodeIds.get(componentIndex),
                            mergedNodeIdOfComponent[componentIndex], orderGraph));
                }
            } else {
                // Keep the node data, but assign its index in the new list.
                newNodeList.add(new GraphNode<>(oldNode.entity(), oldNode.variableReferences(), newNodeList.size(),
                        oldNode.entityId(), oldNode.groupEntityIds(), oldNode.hasDynamicEdges()));
            }
            oldNodeIdToNewNodeId[oldNodeId] = isMerged ? mergedNodeIdOfComponent[componentIndex]
                    : newNodeList.size() - 1;
        }
        replaceEdges(oldNodeIdToNewNodeId, newNodeList);
        replaceContainingNodeMap(newNodeList);
        nodeList.clear();
        nodeList.addAll(newNodeList);
    }

    private GraphNode<Solution_> createMergedNode(BitSet component, int newNodeId,
            DefaultTopologicalOrderGraph orderGraph) {
        var sortedNodeIds = component.stream().boxed().sorted(Comparator
                .comparingInt(orderGraph::getTopologicalOrder)
                .thenComparingInt(Integer::intValue)).collect(Collectors.toCollection(ArrayList::new));
        var mergedUpdaters = new ArrayList<VariableUpdaterInfo<Solution_>>();
        for (var nodeId : sortedNodeIds) {
            mergedUpdaters.addAll(nodeList.get(nodeId).variableReferences());
        }
        var firstNode = nodeList.get(component.nextSetBit(0));
        return new GraphNode<>(firstNode.entity(), mergedUpdaters, newNodeId, firstNode.entityId(), null,
                firstNode.hasDynamicEdges());
    }

    private void replaceEdges(int[] oldNodeIdToNewNodeId, List<GraphNode<Solution_>> newNodeList) {
        var newFixedEdges = new HashMap<GraphNode<Solution_>, List<GraphNode<Solution_>>>();
        remapEdges(fixedEdges, oldNodeIdToNewNodeId, newNodeList, newFixedEdges);
        fixedEdges.clear();
        fixedEdges.putAll(newFixedEdges);
        var newInitialDynamicEdges = new HashMap<GraphNode<Solution_>, List<GraphNode<Solution_>>>();
        remapEdges(initialDynamicEdges, oldNodeIdToNewNodeId, newNodeList, newInitialDynamicEdges);
        initialDynamicEdges.clear();
        initialDynamicEdges.putAll(newInitialDynamicEdges);
    }

    private void remapEdges(Map<GraphNode<Solution_>, List<GraphNode<Solution_>>> edgeMap,
            int[] oldNodeIdToNewNodeId, List<GraphNode<Solution_>> newNodeList,
            Map<GraphNode<Solution_>, List<GraphNode<Solution_>>> newEdgeMap) {
        for (var edgeEntry : edgeMap.entrySet()) {
            var fromNode = newNodeList.get(oldNodeIdToNewNodeId[edgeEntry.getKey().graphNodeId()]);
            for (var toNode : edgeEntry.getValue()) {
                var newToNodeId = oldNodeIdToNewNodeId[toNode.graphNodeId()];
                if (fromNode.graphNodeId() == newToNodeId) {
                    continue;
                }
                var toList = newEdgeMap.computeIfAbsent(fromNode, ignored -> new ArrayList<>());
                var newToNode = newNodeList.get(newToNodeId);
                // Do not deduplicate; dynamic edges that are merged should call `addEdge`
                // twice so the removal of one keeps the edge present in the graph
                // (duplicates largely don't matter since AbstractVariableReferenceGraph keep counts anyway)
                toList.add(newToNode);
            }
        }
    }

    private void replaceContainingNodeMap(List<GraphNode<Solution_>> newNodeList) {
        var newContainingNodeMap = new HashMap<VariableMetaModel<?, ?, ?>, Map<Object, GraphNode<Solution_>>>();
        for (var node : newNodeList) {
            var entityList = node.groupEntityIds() == null
                    ? List.of(node.entity())
                    : List.of(node.variableReferences().getFirst().groupEntities());
            for (var updater : node.variableReferences()) {
                for (var entity : entityList) {
                    newContainingNodeMap.computeIfAbsent(updater.id(), ignored -> new IdentityHashMap<>())
                            .put(entity, node);
                }
            }
        }
        variableReferenceToContainingNodeMap.clear();
        variableReferenceToContainingNodeMap.putAll(newContainingNodeMap);
    }

    private void assertNoFixedLoops() {
        var graph = new DefaultTopologicalOrderGraph(nodeList.size());
        for (var fixedEdge : fixedEdges.entrySet()) {
            var fromNodeId = fixedEdge.getKey().graphNodeId();
            for (var toNode : fixedEdge.getValue()) {
                var toNodeId = toNode.graphNodeId();
                graph.addEdge(fromNodeId, toNodeId);
            }
        }

        var changedBitSet = new BitSet();
        graph.commitChanges(changedBitSet);

        if (changedBitSet.cardinality() == 0) {
            // No node's loop status changed, so the graph does
            // not have any fixed loops.
            return;
        }

        // At least one node's loop status has changed,
        // and since the empty graph has no loops, that
        // mean there is at least one fixed loop in the graph.
        var loopedComponents = graph.getLoopedComponentList();
        var limit = 3;
        var isLimited = loopedComponents.size() > limit;
        var loopedVariables = new LinkedHashSet<VariableMetaModel<?, ?, ?>>();
        var nodeCycleList = loopedComponents.stream()
                .map(nodeIds -> nodeIds.stream().mapToInt(Integer::intValue).mapToObj(nodeList::get).toList())
                .toList();

        for (var cycle : nodeCycleList) {
            cycle.stream().flatMap(node -> node.variableReferences().stream())
                    .map(VariableUpdaterInfo::id)
                    .forEach(loopedVariables::add);
        }

        var out = new StringBuilder("There are fixed dependency loops in the graph for variables %s:%n"
                .formatted(loopedVariables));

        for (var cycle : nodeCycleList) {
            out.append(cycle.stream()
                    .map(GraphNode::toString)
                    .collect(Collectors.joining(", ",
                            "- [",
                            "] ")));
        }

        if (isLimited) {
            out.append("- ...(");
            out.append(loopedComponents.size() - limit);
            out.append(" more)%n");
        }
        out.append(
                """

                        Fixed dependency loops indicate a problem in either the input problem or in the @%s of the looped @%s.
                        There are two kinds of fixed dependency loops:

                        - You have two shadow variables whose sources refer to each other;
                          this is called a source-induced fixed loop.
                          In code, this situation looks like this:

                              @ShadowVariable(supplierName="variable1Supplier")
                              String variable1;

                              @ShadowVariable(supplierName="variable2Supplier")
                              String variable2;

                              // ...

                              @ShadowSources("variable2")
                              String variable1Supplier() { /* ... */ }

                              @ShadowSources("variable1")
                              String variable2Supplier() { /* ... */ }

                        - You have a shadow variable whose sources refer to itself transitively via a fact;
                          this is called a fact-induced fixed loop.
                          In code, this situation looks like this:

                              @PlanningEntity
                              public class Entity {
                                  Entity dependency;

                                  @ShadowVariable(supplierName="variableSupplier")
                                  String variable;

                                  @ShadowSources("dependency.variable")
                                  String variableSupplier() { /* ... */ }
                                  // ...
                              }

                              Entity a = new Entity();
                              Entity b = new Entity();
                              a.setDependency(b);
                              b.setDependency(a);
                              // a depends on b, and b depends on a, which is invalid.


                        The solver cannot break a fixed loop since the loop is caused by sources or facts instead of variables.
                        Fixed loops should not be confused with variable-induced loops, which can be broken by the solver:

                              @PlanningEntity
                              public class Entity {
                                  Entity dependency;

                                  @PreviousElementShadowVariable(/* ... */)
                                  Entity previous;

                                  @ShadowVariable(supplierName="variableSupplier")
                                  String variable;

                                  @ShadowSources({"previous.variable", "dependency.variable"})
                                  String variable1Supplier() { /* ... */ }
                                  // ...
                              }

                              Entity a = new Entity();
                              Entity b = new Entity();
                              b.setDependency(a);
                              a.setPrevious(b);
                              // b depends on a via a fact, and a depends on b via a variable
                              // The solver can break this loop by moving a after b.


                        Maybe check none of your @%s form a loop on the same entity.
                        """
                        .formatted(ShadowSources.class.getSimpleName(), ShadowVariable.class.getSimpleName(),
                                ShadowSources.class.getSimpleName()));
        throw new IllegalArgumentException(out.toString());
    }

}
