package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;

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
    private final DefaultEdge[][] edges;
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
        edges = new DefaultEdge[instanceCount][instanceCount];
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

        var edge = edges[fromNodeId][toNodeId];
        if (edge == null) {
            edge = new DefaultEdge(fromNodeId, toNodeId);
            edges[fromNodeId][toNodeId] = edge;
            graph.addEdge(edge);
        }
        edge.increaseCount();

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

        var edge = edges[fromNodeId][toNodeId];
        if (edge.decreaseCount() == 0) {
            graph.removeEdge(edge);
            edges[fromNodeId][toNodeId] = null;
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
        var builder = new StringBuilder("{\n");
        for (int from = 0; from < edges.length; from++) {
            var row = edges[from];
            var first = true;
            for (int to = 0; to < row.length; to++) {
                var edge = row[to];
                if (edge != null) {
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

    public static final class DefaultEdge implements TopologicalOrderGraph.Edge {

        private final int from;
        private final int to;
        private int count = 0;

        public DefaultEdge(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public int from() {
            return from;
        }

        @Override
        public int to() {
            return to;
        }

        public void increaseCount() {
            count++;
        }

        public int decreaseCount() {
            return --count;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof DefaultEdge other &&
                    from == other.from &&
                    to == other.to;
        }

        @Override
        public int hashCode() {
            var hash = 31;
            hash += 31 * from;
            hash += 31 * to * to; // Make sure order of nodes matters.
            return hash;
        }

        @Override
        public String toString() {
            return "%d->%d".formatted(from, to);
        }
    }

}
