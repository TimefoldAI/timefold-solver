package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

public class VariableReferenceGraph<Solution_> {
    private final Map<VariableId, Map<Object, EntityVariableOrFactReference<Solution_>>> variableReferenceToInstanceMap;
    private final List<EntityVariableOrFactReference<Solution_>> instanceList;
    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;

    private final Map<VariableId, List<BiConsumer<VariableReferenceGraph<Solution_>, Object>>> variableReferenceToBeforeProcessor;
    private final Map<VariableId, List<BiConsumer<VariableReferenceGraph<Solution_>, Object>>> variableReferenceToAfterProcessor;
    private final Map<VariableId, AbstractShadowVariableReference<Solution_, ?, ?>> variableIdToShadowVariable;
    private final Map<EntityVariableOrFactReference<Solution_>, List<EntityVariableOrFactReference<Solution_>>> fixedEdges;
    private final IdentityHashMap<Object, List<EntityVariableOrFactReference<Solution_>>> entityToVariableReferenceMap;
    private int[][] counts;
    private TopologicalOrderGraph graph;
    private BitSet changed;
    private boolean updating = false;

    public VariableReferenceGraph(ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        this.changedVariableNotifier = changedVariableNotifier;
        variableReferenceToInstanceMap = new HashMap<>();
        instanceList = new ArrayList<>();
        variableReferenceToBeforeProcessor = new HashMap<>();
        variableReferenceToAfterProcessor = new HashMap<>();
        variableIdToShadowVariable = new HashMap<>();
        fixedEdges = new HashMap<>();
        entityToVariableReferenceMap = new IdentityHashMap<>();
    }

    public void addShadowVariable(AbstractShadowVariableReference<Solution_, ?, ?> shadowVariable) {
        variableIdToShadowVariable.put(shadowVariable.getVariableId(), shadowVariable);
    }

    public EntityVariableOrFactReference<Solution_> addVariableReferenceEntity(VariableId variableId, Object entity,
            InnerVariableReference<Solution_, ?, ?> variableReference) {
        if (!variableId.entityClass().isInstance(entity)) {
            throw new IllegalArgumentException(variableId + " " + entity);
        }
        if (variableReferenceToInstanceMap.containsKey(variableId) &&
                variableReferenceToInstanceMap.get(variableId).containsKey(entity)) {
            return variableReferenceToInstanceMap.get(variableId).get(entity);
        }
        var node = new EntityVariableOrFactReference<>(variableId, entity, variableReference, instanceList.size());
        variableReferenceToInstanceMap.computeIfAbsent(variableId, k -> new IdentityHashMap<>())
                .put(entity, node);
        instanceList.add(node);
        return node;
    }

    public void addBeforeProcessor(VariableId parentVariableId,
            BiConsumer<VariableReferenceGraph<Solution_>, Object> consumer) {
        variableReferenceToBeforeProcessor.computeIfAbsent(parentVariableId, k -> new ArrayList<>())
                .add(consumer);
    }

    public void addAfterProcessor(VariableId parentVariableId,
            BiConsumer<VariableReferenceGraph<Solution_>, Object> consumer) {
        variableReferenceToAfterProcessor.computeIfAbsent(parentVariableId, k -> new ArrayList<>())
                .add(consumer);
    }

    public void createGraph(IntFunction<TopologicalOrderGraph> graphCreator) {
        counts = new int[instanceList.size()][instanceList.size()];
        graph = graphCreator.apply(instanceList.size());
        graph.withNodeData(instanceList);
        changed = new BitSet(instanceList.size());

        for (var instance : instanceList) {
            afterVariableChanged(instance.variableId(), instance.entity());
            entityToVariableReferenceMap.computeIfAbsent(instance.entity(), ignored -> new ArrayList<>())
                    .add(instance);
        }
        for (var fixedEdgeEntry : fixedEdges.entrySet()) {
            for (var toEdge : fixedEdgeEntry.getValue()) {
                addEdge(fixedEdgeEntry.getKey(), toEdge);
            }
        }
    }

    public EntityVariableOrFactReference<Solution_> lookup(VariableId variableReference, Object entity) {
        return variableReferenceToInstanceMap.getOrDefault(variableReference, Collections.emptyMap()).get(entity);
    }

    public void addFixedEdge(EntityVariableOrFactReference<Solution_> from, EntityVariableOrFactReference<Solution_> to) {
        if (from.id() == to.id()) {
            return;
        }
        fixedEdges.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    }

    public void addEdge(EntityVariableOrFactReference<Solution_> from, EntityVariableOrFactReference<Solution_> to) {
        if (from.id() == to.id()) {
            return;
        }
        if (changed.isEmpty()) {
            graph.startBatchChange();
        }
        var oldCount = counts[from.id()][to.id()]++;
        if (oldCount == 0) {
            graph.addEdge(from.id(), to.id());
        }
        markChanged(to);
    }

    public void removeEdge(EntityVariableOrFactReference<Solution_> from, EntityVariableOrFactReference<Solution_> to) {
        if (from.id() == to.id()) {
            return;
        }
        if (changed.isEmpty()) {
            graph.startBatchChange();
        }
        var newCount = --counts[from.id()][to.id()];
        if (newCount == 0) {
            graph.removeEdge(from.id(), to.id());
        }
        markChanged(to);
    }

    public void markChanged(EntityVariableOrFactReference<Solution_> node) {
        if (changed.isEmpty()) {
            graph.startBatchChange();
        }
        changed.set(node.id());
    }

    public void updateChanged() {
        if (updating) {
            // An exception occurred when processing a move
            // Variable state is probably incorrect/will trigger a different
            // exception, so return early.
            return;
        }
        graph.endBatchChange();

        updating = true;
        var visited = new boolean[instanceList.size()];
        var loopedTracker = new LoopedTracker(visited.length);

        while (!changed.isEmpty()) {
            int minNode = popChangedNodeWithLowestTopologicalIndex();
            if (visited[minNode]) {
                continue;
            }
            visited[minNode] = true;
            var shadowVariable = variableIdToShadowVariable.get(instanceList.get(minNode).variableId());
            var isChanged = true;

            if (shadowVariable != null) {
                var isEntityLooped = entityToVariableReferenceMap.get(instanceList.get(minNode).entity())
                        .stream().anyMatch(instance -> graph.isLooped(loopedTracker, instance.id()));
                if (isEntityLooped) {
                    shadowVariable.invalidate(changedVariableNotifier, instanceList.get(minNode).entity());
                } else {
                    isChanged = shadowVariable.update(changedVariableNotifier, instanceList.get(minNode).entity());
                }
            }

            if (isChanged) {
                graph.nodeForwardEdges(minNode).forEachRemaining((int node) -> {
                    changed.set(node);
                });
            }
        }
        updating = false;
    }

    private int popChangedNodeWithLowestTopologicalIndex() {
        int minTopologicalOrder = Integer.MAX_VALUE;
        int minNode = 0;
        for (int i = changed.nextSetBit(0); i >= 0; i = changed.nextSetBit(i + 1)) {
            var topologicalOrder = graph.getTopologicalOrder(i);
            if (topologicalOrder < minTopologicalOrder) {
                minTopologicalOrder = topologicalOrder;
                minNode = i;
            }
            if (i == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
        }
        changed.clear(minNode);
        return minNode;
    }

    public void beforeVariableChanged(VariableId variableReference, Object entity) {
        if (variableReference.entityClass().isInstance(entity)) {
            var updaterList = variableReferenceToBeforeProcessor.getOrDefault(variableReference, Collections.emptyList());
            for (var consumer : updaterList) {
                consumer.accept(this, entity);
            }
        }
    }

    public void afterVariableChanged(VariableId variableReference, Object entity) {
        if (variableReference.entityClass().isInstance(entity)) {
            var updaterList = variableReferenceToAfterProcessor.getOrDefault(variableReference, Collections.emptyList());
            var node = lookup(variableReference, entity);
            if (node != null) {
                markChanged(node);
            }
            for (var consumer : updaterList) {
                consumer.accept(this, entity);
            }
        }
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
}
