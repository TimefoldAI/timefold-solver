package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.domain.solution.descriptor.InnerVariableMetaModel;
import ai.timefold.solver.core.impl.util.DynamicLinearProbeNonNegativeIntCounter;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract sealed class AbstractVariableReferenceGraph<Solution_, ChangeTracker_> implements VariableReferenceGraph
        permits DefaultVariableReferenceGraph, FixedVariableReferenceGraph {

    // These structures are immutable.
    protected final List<GraphNode<Solution_>> nodeList;
    protected final BaseTopologicalOrderGraph.NodeTopologicalOrder[] nodeTopologicalOrders;
    protected final Map<VariableMetaModel<?, ?, ?>, Map<Object, GraphNode<Solution_>>> variableReferenceToContainingNodeMap;
    protected final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>>> variableReferenceToBeforeProcessor;
    protected final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>>> variableReferenceToAfterProcessor;
    protected final Map<VariableMetaModel<?, ?, ?>, List<ListElementSourceLocator>> listVariableReferenceToElementLocator;
    private final Map<VariableMetaModel<?, ?, ?>, VariableChangeHook> variableReferenceToHookMap;

    // These structures are mutable.
    protected final DynamicLinearProbeNonNegativeIntCounter[] edgeCount;
    protected final ChangeTracker_ changeTracker;
    protected final TopologicalOrderGraph graph;

    /**
     * True if we are currently doing a declarative shadow variable update,
     * false otherwise.
     */
    protected boolean isUpdating;

    AbstractVariableReferenceGraph(VariableReferenceGraphBuilder<Solution_> outerGraph,
            IntFunction<TopologicalOrderGraph> graphCreator) {
        isUpdating = false;
        nodeList = List.copyOf(outerGraph.nodeList);
        var instanceCount = nodeList.size();
        // Often the maps are a singleton; we improve performance by actually making it so.
        variableReferenceToContainingNodeMap = Map.copyOf(outerGraph.variableReferenceToContainingNodeMap);
        variableReferenceToBeforeProcessor = Map.copyOf(outerGraph.variableReferenceToBeforeProcessor);
        variableReferenceToAfterProcessor = Map.copyOf(outerGraph.variableReferenceToAfterProcessor);
        listVariableReferenceToElementLocator = Map.copyOf(outerGraph.listVariableReferenceToElementLocator);
        edgeCount = new DynamicLinearProbeNonNegativeIntCounter[instanceCount];
        for (var i = 0; i < instanceCount; i++) {
            edgeCount[i] = new DynamicLinearProbeNonNegativeIntCounter();
        }
        graph = graphCreator.apply(instanceCount);
        graph.withNodeData(nodeList);
        nodeTopologicalOrders = buildNodeTopologicalOrderArray(graph, nodeList.size());

        var visited = Collections.newSetFromMap(new IdentityHashMap<>());
        changeTracker = createChangeTracker(instanceCount);
        variableReferenceToHookMap = buildHookMap();
        var initialHookList = variableReferenceToAfterProcessor.keySet()
                .stream()
                .map(variableReferenceToHookMap::get)
                .toList();
        for (var instance : nodeList) {
            var entity = instance.entity();
            if (visited.add(entity)) {
                for (var hook : initialHookList) {
                    hook.afterVariableChanged(entity);
                }
            }
        }
        for (var fixedEdgeEntry : outerGraph.fixedEdges.entrySet()) {
            for (var toEdge : fixedEdgeEntry.getValue()) {
                addEdge(fixedEdgeEntry.getKey(), toEdge);
            }
        }
        for (var initialDynamicEdgeEntry : outerGraph.initialDynamicEdges.entrySet()) {
            for (var toEdge : initialDynamicEdgeEntry.getValue()) {
                addEdge(initialDynamicEdgeEntry.getKey(), toEdge);
            }
        }
    }

    /**
     * As specified by {@link VariableReferenceGraph#updateChanged()}.
     *
     * @return true if the update successful; false otherwise
     * @implNote {@link #updateChanged()} sets {{@link #isUpdating}} to true
     *           so {@link VariableChangeHook}s can short circuit.
     */
    abstract boolean innerUpdateChanged();

    /**
     * Called when any non-declarative source variable for the
     * given {@link GraphNode} changes.
     *
     * @param changed The graph node that has a non-declarative source variable that changed.
     */
    abstract void markChanged(GraphNode<Solution_> changed);

    @Override
    public final boolean updateChanged() {
        isUpdating = true;
        var success = innerUpdateChanged();
        isUpdating = false;
        return success;
    }

    private BaseTopologicalOrderGraph.NodeTopologicalOrder[] buildNodeTopologicalOrderArray(BaseTopologicalOrderGraph graph,
            int graphSize) {
        var out = new BaseTopologicalOrderGraph.NodeTopologicalOrder[graphSize];
        for (var i = 0; i < out.length; i++) {
            out[i] = new BaseTopologicalOrderGraph.NodeTopologicalOrder(i, graph);
        }
        return out;
    }

    /**
     * Create the data structure used by {@link #markChanged(GraphNode)}.
     * Used in the constructor when constructing the initial graph.
     *
     * @param instanceCount The number of nodes in the graph
     * @return The data structure used for tracking.
     */
    protected abstract ChangeTracker_ createChangeTracker(int instanceCount);

    public final @Nullable GraphNode<Solution_> lookupOrNull(VariableMetaModel<?, ?, ?> variableId, Object entity) {
        var map = variableReferenceToContainingNodeMap.get(variableId);
        if (map == null) {
            return null;
        }
        return map.get(entity);
    }

    public final void addEdge(@NonNull GraphNode<Solution_> from, @NonNull GraphNode<Solution_> to) {
        var fromNodeId = from.graphNodeId();
        var toNodeId = to.graphNodeId();
        if (fromNodeId == toNodeId) {
            return;
        }

        var count = edgeCount[fromNodeId].getCount(toNodeId);
        if (count == 0) {
            graph.addEdge(fromNodeId, toNodeId);
        }
        edgeCount[fromNodeId].increment(toNodeId);
        markChanged(to);
    }

    public final void removeEdge(@NonNull GraphNode<Solution_> from, @NonNull GraphNode<Solution_> to) {
        var fromNodeId = from.graphNodeId();
        var toNodeId = to.graphNodeId();
        if (fromNodeId == toNodeId) {
            return;
        }

        var count = edgeCount[fromNodeId].getCount(toNodeId);
        if (count == 1) {
            graph.removeEdge(fromNodeId, toNodeId);
        }
        edgeCount[fromNodeId].decrement(toNodeId);
        markChanged(to);
    }

    private Map<VariableMetaModel<?, ?, ?>, VariableChangeHook> buildHookMap() {
        var hookMap = new LinkedHashMap<VariableMetaModel<?, ?, ?>, VariableChangeHook>();
        for (var variableReferenceSet : List.of(variableReferenceToContainingNodeMap.keySet(),
                variableReferenceToBeforeProcessor.keySet(), variableReferenceToAfterProcessor.keySet(),
                listVariableReferenceToElementLocator.keySet())) {
            for (var variableReference : variableReferenceSet) {
                hookMap.computeIfAbsent(variableReference, this::buildHook);
            }
        }
        return hookMap;
    }

    private VariableChangeHook buildHook(VariableMetaModel<?, ?, ?> variableReference) {
        var nodeMap = variableReferenceToContainingNodeMap.get(variableReference);
        var beforeProcessorList =
                variableReferenceToBeforeProcessor.getOrDefault(variableReference, Collections.emptyList());
        var afterProcessorList =
                variableReferenceToAfterProcessor.getOrDefault(variableReference, Collections.emptyList());
        var locatorList = resolveLocators(listVariableReferenceToElementLocator.get(variableReference));
        // Equal metamodels may come from different subclasses; the declaring entity class accepts all of them.
        var entityType = ((InnerVariableMetaModel<?>) variableReference).variableDescriptor().getEntityDescriptor()
                .getEntityClass();
        return new ResolvedVariableChangeHook(entityType, nodeMap, beforeProcessorList,
                afterProcessorList, locatorList);
    }

    private List<ResolvedLocator<Solution_>> resolveLocators(@Nullable List<ListElementSourceLocator> locatorList) {
        if (locatorList == null) {
            return Collections.emptyList();
        }
        var resolvedLocatorList = new ArrayList<ResolvedLocator<Solution_>>(locatorList.size());
        for (var locator : locatorList) {
            var targetNodeMap = variableReferenceToContainingNodeMap.get(locator.targetVariableId());
            if (targetNodeMap != null) { // Otherwise the target is never found.
                resolvedLocatorList.add(new ResolvedLocator<>(locator, targetNodeMap,
                        variableReferenceToContainingNodeMap.get(locator.sourceVariableId())));
            }
        }
        return resolvedLocatorList;
    }

    @Override
    public final @Nullable VariableChangeHook resolveHookFor(VariableMetaModel<?, ?, ?> variableReference) {
        return variableReferenceToHookMap.get(variableReference);
    }

    private final class ResolvedVariableChangeHook implements VariableChangeHook {

        private final Class<?> entityType;
        private final @Nullable Map<Object, GraphNode<Solution_>> nodeMap;
        private final List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>> beforeProcessorList;
        private final List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>> afterProcessorList;
        private final List<ResolvedLocator<Solution_>> locatorList;

        private ResolvedVariableChangeHook(Class<?> entityType, @Nullable Map<Object, GraphNode<Solution_>> nodeMap,
                List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>> beforeProcessorList,
                List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>> afterProcessorList,
                List<ResolvedLocator<Solution_>> locatorList) {
            this.entityType = entityType;
            this.nodeMap = nodeMap;
            this.beforeProcessorList = beforeProcessorList;
            this.afterProcessorList = afterProcessorList;
            this.locatorList = locatorList;
        }

        @Override
        public void beforeVariableChanged(Object entity) {
            if (isUpdating) {
                // If we are updating, then the variable that changed is a declarative shadow variable;
                // We don't need to check for graph modifications/track changes when we are updating, so skip
                return;
            }
            if (entityType.isInstance(entity)) {
                processEntity(beforeProcessorList, entity);
            }
        }

        @SuppressWarnings("ForLoopReplaceableByForEach")
        private void processEntity(List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>> processorList,
                Object entity) {
            var processorCount = processorList.size();
            // Avoid creation of iterators on the hot path.
            // The short-lived instances were observed to cause considerable GC pressure.
            for (var i = 0; i < processorCount; i++) {
                processorList.get(i).accept(AbstractVariableReferenceGraph.this, entity);
            }
        }

        @Override
        public void afterVariableChanged(Object entity) {
            if (isUpdating) {
                // If we are updating, then the variable that changed is a declarative shadow variable;
                // We don't need to check for graph modifications/track changes when we are updating, so skip
                return;
            }
            if (entityType.isInstance(entity)) {
                if (nodeMap != null) {
                    var node = nodeMap.get(entity);
                    if (node != null) {
                        markChanged(node);
                    }
                }
                processEntity(afterProcessorList, entity);
            }
        }

        @Override
        public void beforeListVariableChanged(Object entity, List<Object> elementsBeforeChange, int fromIndex, int toIndex) {
            updateListElementEdges(locatorList, entity, elementsBeforeChange, fromIndex, toIndex, false);
        }

        @SuppressWarnings("ForLoopReplaceableByForEach")
        private void updateListElementEdges(List<ResolvedLocator<Solution_>> locatorList, Object entity,
                List<Object> elementList, int fromIndex, int toIndex, boolean isAdd) {
            var locatorCount = locatorList.size();
            for (var i = 0; i < locatorCount; i++) {
                var resolvedLocator = locatorList.get(i);
                var to = resolvedLocator.targetNodeMap().get(entity);
                if (to == null) {
                    continue;
                }
                var locator = resolvedLocator.locator();
                var sourceNodeMap = resolvedLocator.sourceNodeMap();
                if (sourceNodeMap != null) {
                    // Do not clamp the range; an out-of-bounds range is a caller bug
                    // that must fail fast instead of silently corrupting the edge counts.
                    for (var elementIndex = fromIndex; elementIndex < toIndex; elementIndex++) {
                        var sourceEntity = locator.findSourceEntity(elementList.get(elementIndex));
                        if (sourceEntity == null) {
                            continue;
                        }
                        var from = sourceNodeMap.get(sourceEntity);
                        if (from == null) {
                            continue;
                        }
                        if (isAdd) {
                            addEdge(from, to);
                        } else {
                            removeEdge(from, to);
                        }
                    }
                }
                if (isAdd) {
                    // The dependency set changed even if the range is empty (e.g. an element was removed),
                    // so the target variable must always be recomputed.
                    // At graph construction, the same is guaranteed by the after processor registered in
                    // DefaultShadowVariableSessionFactory.createListElementSourceProcessors().
                    markChanged(to);
                }
            }
        }

        @Override
        public void afterListVariableChanged(Object entity, List<Object> elementsAfterChange, int fromIndex, int toIndex) {
            updateListElementEdges(locatorList, entity, elementsAfterChange, fromIndex, toIndex, true);
        }

    }

    private record ResolvedLocator<Solution_>(ListElementSourceLocator locator,
            Map<Object, GraphNode<Solution_>> targetNodeMap,
            @Nullable Map<Object, GraphNode<Solution_>> sourceNodeMap) {
    }

    @Override
    public String toString() {
        var edgeList = new LinkedHashMap<GraphNode<Solution_>, List<GraphNode<Solution_>>>();
        graph.forEachEdge((from, to) -> edgeList.computeIfAbsent(nodeList.get(from), k -> new ArrayList<>())
                .add(nodeList.get(to)));
        return edgeList.entrySet()
                .stream()
                .map(e -> e.getKey() + "->" + e.getValue())
                .collect(Collectors.joining(
                        "," + System.lineSeparator() + " ",
                        "{" + System.lineSeparator() + "  ",
                        "}"));

    }

}
