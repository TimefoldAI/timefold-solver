package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class DefaultWorkingReferenceGraph<Solution_> implements WorkingReferenceGraph<Solution_> {

    // These structures are immutable.
    private final List<EntityVariablePair<Solution_>> instanceList;
    private final Map<VariableMetaModel<?, ?, ?>, Map<Object, EntityVariablePair<Solution_>>> variableReferenceToInstanceMap;

    // These structures are mutable.
    private final int[][] counts;
    private final TopologicalOrderGraph graph;
    private final BitSet changed;

    private final Consumer<BitSet> affectedEntitiesUpdater;

    public DefaultWorkingReferenceGraph(VariableReferenceGraph<Solution_> outerGraph,
            IntFunction<TopologicalOrderGraph> graphCreator) {
        instanceList = List.copyOf(outerGraph.instanceList);
        var instanceCount = instanceList.size();
        // Often the map is a singleton; we improve performance by actually making it so.
        variableReferenceToInstanceMap = mapOfMapsDeepCopyOf(outerGraph.variableReferenceToInstanceMap);
        counts = new int[instanceCount][instanceCount];
        graph = graphCreator.apply(instanceCount);
        graph.withNodeData(instanceList);
        changed = new BitSet(instanceCount);

        var entityToVariableReferenceMap = new IdentityHashMap<Object, List<EntityVariablePair<Solution_>>>();
        graph.startBatchChange();
        var visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var instance : instanceList) {
            var entity = instance.entity();
            if (visited.add(entity)) {
                for (var variableId : outerGraph.variableReferenceToAfterProcessor.keySet()) {
                    outerGraph.afterVariableChanged(this, variableId, entity);
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
        return variableReferenceToInstanceMap.getOrDefault(variableId, Collections.emptyMap()).get(entity);
    }

    @Override
    public void addEdge(@NonNull EntityVariablePair<Solution_> from, @NonNull EntityVariablePair<Solution_> to) {
        var fromNodeId = from.graphNodeId();
        var toNodeId = to.graphNodeId();
        if (fromNodeId == toNodeId) {
            return;
        }

        if (changed.isEmpty()) {
            graph.startBatchChange();
        }

        var oldCount = counts[fromNodeId][toNodeId]++;
        if (oldCount == 0) {
            graph.addEdge(fromNodeId, toNodeId);
        }

        markChanged(to);
    }

    @Override
    public void removeEdge(@NonNull EntityVariablePair<Solution_> from, @NonNull EntityVariablePair<Solution_> to) {
        var fromNodeId = from.graphNodeId();
        var toNodeId = to.graphNodeId();
        if (fromNodeId == toNodeId) {
            return;
        }

        if (changed.isEmpty()) {
            graph.startBatchChange();
        }

        var newCount = --counts[fromNodeId][toNodeId];
        if (newCount == 0) {
            graph.removeEdge(fromNodeId, toNodeId);
        }

        markChanged(to);
    }

    @Override
    public void markChanged(@NonNull EntityVariablePair<Solution_> node) {
        if (changed.isEmpty()) {
            graph.startBatchChange();
        }
        changed.set(node.graphNodeId());
    }

    @Override
    public void updateChanged() {
        if (changed.isEmpty()) {
            return;
        }
        graph.endBatchChange();
        affectedEntitiesUpdater.accept(changed);
    }

    @Override
    public String toString() {
        var builder = new StringBuilder("{\n");
        for (int from = 0; from < counts.length; from++) {
            var first = true;
            for (int to = 0; to < counts.length; to++) {
                if (counts[from][to] != 0) {
                    if (first) {
                        first = false;
                        builder.append("    \"").append(instanceList.get(from)).append("\": [");
                    } else {
                        builder.append(", ");
                    }
                    builder.append("\"%s\"".formatted(instanceList.get(to)));
                }
            }
            if (!first) {
                builder.append("],\n");
            }
        }
        builder.append("}");
        return builder.toString();
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
