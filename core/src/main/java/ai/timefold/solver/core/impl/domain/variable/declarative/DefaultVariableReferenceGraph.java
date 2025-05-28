package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.util.DynamicIntArray;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class DefaultVariableReferenceGraph<Solution_> implements VariableReferenceGraph<Solution_> {

    // These structures are immutable.
    private final List<EntityVariablePair<Solution_>> instanceList;
    private final Map<VariableMetaModel<?, ?, ?>, Map<Object, EntityVariablePair<Solution_>>> variableReferenceToInstanceMap;
    private final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<VariableReferenceGraph<Solution_>, Object>>> variableReferenceToBeforeProcessor;
    private final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<VariableReferenceGraph<Solution_>, Object>>> variableReferenceToAfterProcessor;

    // These structures are mutable.
    private final DynamicIntArray[] edgeCount;
    private final TopologicalOrderGraph graph;
    private final BitSet changed;

    private final Consumer<BitSet> affectedEntitiesUpdater;

    public DefaultVariableReferenceGraph(VariableReferenceGraphBuilder<Solution_> outerGraph,
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
        changed = new BitSet(instanceCount);

        var entityToVariableReferenceMap = new IdentityHashMap<Object, List<EntityVariablePair<Solution_>>>();
        var visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var instance : instanceList) {
            var entity = instance.entity();
            if (visited.add(entity)) {
                for (var variableId : outerGraph.variableReferenceToAfterProcessor.keySet()) {
                    afterVariableChanged(variableId, entity);
                }
            }
            entityToVariableReferenceMap.computeIfAbsent(entity, ignored -> new ArrayList<>())
                    .add(instance);
        }
        for (var fixedEdgeEntry : outerGraph.fixedEdges.entrySet()) {
            for (var toEdge : fixedEdgeEntry.getValue()) {
                addEdge(fixedEdgeEntry.getKey(), toEdge);
            }
        }
        // Immutable optimized version of the map, now that it won't be updated anymore.
        var immutableEntityToVariableReferenceMap = mapOfListsDeepCopyOf(entityToVariableReferenceMap);
        // This mutable structure is created once, and reused from there on.
        // Otherwise its internal collections were observed being re-created so often
        // that the allocation of arrays would become a major bottleneck.
        affectedEntitiesUpdater = new AffectedEntitiesUpdater<>(graph, instanceList, immutableEntityToVariableReferenceMap::get,
                outerGraph.changedVariableNotifier);
    }

    @Override
    public @Nullable EntityVariablePair<Solution_> lookupOrNull(VariableMetaModel<?, ?, ?> variableId, Object entity) {
        var map = variableReferenceToInstanceMap.get(variableId);
        if (map == null) {
            return null;
        }
        return map.get(entity);
    }

    @Override
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

    @Override
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

    @Override
    public void markChanged(@NonNull EntityVariablePair<Solution_> node) {
        changed.set(node.graphNodeId());
    }

    @Override
    public void updateChanged() {
        if (changed.isEmpty()) {
            return;
        }
        graph.commitChanges();
        affectedEntitiesUpdater.accept(changed);
    }

    @Override
    public void beforeVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        if (variableReference.entity().type().isInstance(entity)) {
            processEntity(variableReferenceToBeforeProcessor.getOrDefault(variableReference, Collections.emptyList()), entity);
        }
    }

    private void processEntity(List<BiConsumer<VariableReferenceGraph<Solution_>, Object>> processorList, Object entity) {
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
    private static <K1, K2, V> Map<K1, Map<K2, V>> mapOfMapsDeepCopyOf(Map<K1, Map<K2, V>> map) {
        var entryArray = map.entrySet()
                .stream()
                .map(e -> Map.entry(e.getKey(), Map.copyOf(e.getValue())))
                .toArray(Map.Entry[]::new);
        return Map.ofEntries(entryArray);
    }

    @SuppressWarnings("unchecked")
    private static <K1, V> Map<K1, List<V>> mapOfListsDeepCopyOf(Map<K1, List<V>> map) {
        var entryArray = map.entrySet()
                .stream()
                .map(e -> Map.entry(e.getKey(), List.copyOf(e.getValue())))
                .toArray(Map.Entry[]::new);
        return Map.ofEntries(entryArray);
    }

}
