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

import ai.timefold.solver.core.impl.util.DynamicIntArray;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract sealed class AbstractVariableReferenceGraph<Solution_, ChangeSet_> implements VariableReferenceGraph
        permits DefaultVariableReferenceGraph, FixedVariableReferenceGraph {

    // These structures are immutable.
    protected final List<EntityVariablePair<Solution_>> instanceList;
    protected final Map<VariableMetaModel<?, ?, ?>, Map<Object, EntityVariablePair<Solution_>>> variableReferenceToInstanceMap;
    protected final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>>> variableReferenceToBeforeProcessor;
    protected final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>>> variableReferenceToAfterProcessor;

    // These structures are mutable.
    protected final DynamicIntArray[] edgeCount;
    protected final ChangeSet_ changeSet;
    protected final TopologicalOrderGraph graph;

    AbstractVariableReferenceGraph(VariableReferenceGraphBuilder<Solution_> outerGraph,
            IntFunction<TopologicalOrderGraph> graphCreator) {
        instanceList = List.copyOf(outerGraph.instanceList);
        var instanceCount = instanceList.size();
        // Often the maps are a singleton; we improve performance by actually making it so.
        variableReferenceToInstanceMap = mapOfMapsDeepCopyOf(outerGraph.variableReferenceToInstanceMap);
        variableReferenceToBeforeProcessor = mapOfListsDeepCopyOf(outerGraph.variableReferenceToBeforeProcessor);
        variableReferenceToAfterProcessor = mapOfListsDeepCopyOf(outerGraph.variableReferenceToAfterProcessor);
        edgeCount = new DynamicIntArray[instanceCount];
        for (int i = 0; i < instanceCount; i++) {
            edgeCount[i] = new DynamicIntArray(instanceCount);
        }
        graph = graphCreator.apply(instanceCount);
        graph.withNodeData(instanceList);

        var visited = Collections.newSetFromMap(new IdentityHashMap<>());
        changeSet = createChangeSet(instanceCount);
        for (var instance : instanceList) {
            var entity = instance.entity();
            if (visited.add(entity)) {
                for (var variableId : outerGraph.variableReferenceToAfterProcessor.keySet()) {
                    afterVariableChanged(variableId, entity);
                }
            }
        }
        for (var fixedEdgeEntry : outerGraph.fixedEdges.entrySet()) {
            for (var toEdge : fixedEdgeEntry.getValue()) {
                addEdge(fixedEdgeEntry.getKey(), toEdge);
            }
        }
    }

    protected abstract ChangeSet_ createChangeSet(int instanceCount);

    public @Nullable EntityVariablePair<Solution_> lookupOrNull(VariableMetaModel<?, ?, ?> variableId, Object entity) {
        var map = variableReferenceToInstanceMap.get(variableId);
        if (map == null) {
            return null;
        }
        return map.get(entity);
    }

    public void addEdge(@NonNull EntityVariablePair<Solution_> from, @NonNull EntityVariablePair<Solution_> to) {
        var fromNodeId = from.graphNodeId();
        var toNodeId = to.graphNodeId();
        if (fromNodeId == toNodeId) {
            return;
        }

        var count = edgeCount[fromNodeId].get(toNodeId);
        if (count == 0) {
            graph.addEdge(fromNodeId, toNodeId);
        }
        edgeCount[fromNodeId].set(toNodeId, count + 1);
        markChanged(to);
    }

    public void removeEdge(@NonNull EntityVariablePair<Solution_> from, @NonNull EntityVariablePair<Solution_> to) {
        var fromNodeId = from.graphNodeId();
        var toNodeId = to.graphNodeId();
        if (fromNodeId == toNodeId) {
            return;
        }

        var count = edgeCount[fromNodeId].get(toNodeId);
        if (count == 1) {
            graph.removeEdge(fromNodeId, toNodeId);
        }
        edgeCount[fromNodeId].set(toNodeId, count - 1);
        markChanged(to);
    }

    abstract void markChanged(EntityVariablePair<Solution_> changed);

    @Override
    public void beforeVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        if (variableReference.entity().type().isInstance(entity)) {
            processEntity(variableReferenceToBeforeProcessor.getOrDefault(variableReference, Collections.emptyList()), entity);
        }
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    private void processEntity(List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>> processorList,
            Object entity) {
        var processorCount = processorList.size();
        // Avoid creation of iterators on the hot path.
        // The short-lived instances were observed to cause considerable GC pressure.
        for (int i = 0; i < processorCount; i++) {
            processorList.get(i).accept(this, entity);
        }
    }

    @Override
    public void afterVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        if (variableReference.entity().type().isInstance(entity)) {
            var node = lookupOrNull(variableReference, entity);
            if (node != null) {
                markChanged(node);
            }
            processEntity(variableReferenceToAfterProcessor.getOrDefault(variableReference, Collections.emptyList()), entity);
        }
    }

    @Override
    public String toString() {
        var edgeList = new LinkedHashMap<EntityVariablePair<Solution_>, List<EntityVariablePair<Solution_>>>();
        graph.forEachEdge((from, to) -> edgeList.computeIfAbsent(instanceList.get(from), k -> new ArrayList<>())
                .add(instanceList.get(to)));
        return edgeList.entrySet()
                .stream()
                .map(e -> e.getKey() + "->" + e.getValue())
                .collect(Collectors.joining(
                        "," + System.lineSeparator() + " ",
                        "{" + System.lineSeparator() + "  ",
                        "}"));

    }

    @SuppressWarnings("unchecked")
    static <K1, K2, V> Map<K1, Map<K2, V>> mapOfMapsDeepCopyOf(Map<K1, Map<K2, V>> map) {
        var entryArray = map.entrySet()
                .stream()
                .map(e -> Map.entry(e.getKey(), Map.copyOf(e.getValue())))
                .toArray(Map.Entry[]::new);
        return Map.ofEntries(entryArray);
    }

    @SuppressWarnings("unchecked")
    static <K1, V> Map<K1, List<V>> mapOfListsDeepCopyOf(Map<K1, List<V>> map) {
        var entryArray = map.entrySet()
                .stream()
                .map(e -> Map.entry(e.getKey(), List.copyOf(e.getValue())))
                .toArray(Map.Entry[]::new);
        return Map.ofEntries(entryArray);
    }

}
