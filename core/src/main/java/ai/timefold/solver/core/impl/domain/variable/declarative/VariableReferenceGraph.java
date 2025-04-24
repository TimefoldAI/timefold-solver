package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

public class VariableReferenceGraph<Solution_> {
    private final Map<VariableMetaModel<?, ?, ?>, Map<Object, EntityVariablePair>> variableReferenceToInstanceMap;
    private final List<EntityVariablePair> instanceList;
    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;

    private final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<VariableReferenceGraph<Solution_>, Object>>> variableReferenceToBeforeProcessor;
    private final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<VariableReferenceGraph<Solution_>, Object>>> variableReferenceToAfterProcessor;
    private final Map<EntityVariablePair, List<EntityVariablePair>> fixedEdges;
    private final IdentityHashMap<Object, List<EntityVariablePair>> entityToVariableReferenceMap;
    private int[][] counts;
    private TopologicalOrderGraph graph;
    private BitSet changed;

    public VariableReferenceGraph(ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        this.changedVariableNotifier = changedVariableNotifier;
        variableReferenceToInstanceMap = new HashMap<>();
        instanceList = new ArrayList<>();
        variableReferenceToBeforeProcessor = new HashMap<>();
        variableReferenceToAfterProcessor = new HashMap<>();
        fixedEdges = new HashMap<>();
        entityToVariableReferenceMap = new IdentityHashMap<>();
    }

    public <Entity_> EntityVariablePair addVariableReferenceEntity(
            VariableMetaModel<?, ?, ?> variableId,
            Entity_ entity,
            VariableUpdaterInfo variableReference) {
        if (variableReferenceToInstanceMap.containsKey(variableId) &&
                variableReferenceToInstanceMap.get(variableId).containsKey(entity)) {
            return variableReferenceToInstanceMap.get(variableId).get(entity);
        }
        var node = new EntityVariablePair(entity, variableId,
                variableReference, instanceList.size());
        variableReferenceToInstanceMap.computeIfAbsent(variableId, k -> new IdentityHashMap<>())
                .put(entity, node);
        instanceList.add(node);
        return node;
    }

    public void addBeforeProcessor(VariableMetaModel<?, ?, ?> variableId,
            BiConsumer<VariableReferenceGraph<Solution_>, Object> consumer) {
        variableReferenceToBeforeProcessor.computeIfAbsent(variableId, k -> new ArrayList<>())
                .add(consumer);
    }

    public void addAfterProcessor(VariableMetaModel<?, ?, ?> variableId,
            BiConsumer<VariableReferenceGraph<Solution_>, Object> consumer) {
        variableReferenceToAfterProcessor.computeIfAbsent(variableId, k -> new ArrayList<>())
                .add(consumer);
    }

    public void createGraph(IntFunction<TopologicalOrderGraph> graphCreator) {
        counts = new int[instanceList.size()][instanceList.size()];
        graph = graphCreator.apply(instanceList.size());
        graph.withNodeData(instanceList);
        changed = new BitSet(instanceList.size());

        graph.startBatchChange();
        var visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var instance : instanceList) {
            if (visited.add(instance.entity())) {
                for (var variableId : variableReferenceToAfterProcessor.keySet()) {
                    if (variableId.entity().type().isInstance(instance.entity())) {
                        afterVariableChanged(variableId, instance.entity());
                    }
                }
            }
            entityToVariableReferenceMap.computeIfAbsent(instance.entity(), ignored -> new ArrayList<>())
                    .add(instance);
        }
        for (var fixedEdgeEntry : fixedEdges.entrySet()) {
            for (var toEdge : fixedEdgeEntry.getValue()) {
                addEdge(fixedEdgeEntry.getKey(), toEdge);
            }
        }
    }

    public EntityVariablePair lookup(VariableMetaModel<?, ?, ?> variableId, Object entity) {
        return variableReferenceToInstanceMap.getOrDefault(variableId, Collections.emptyMap()).get(entity);
    }

    public void addFixedEdge(EntityVariablePair from, EntityVariablePair to) {
        if (from.graphNodeId() == to.graphNodeId()) {
            return;
        }
        fixedEdges.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    }

    public void addEdge(EntityVariablePair from, EntityVariablePair to) {
        if (from.graphNodeId() == to.graphNodeId()) {
            return;
        }
        if (changed.isEmpty()) {
            graph.startBatchChange();
        }
        var oldCount = counts[from.graphNodeId()][to.graphNodeId()]++;
        if (oldCount == 0) {
            graph.addEdge(from.graphNodeId(), to.graphNodeId());
        }

        markChanged(to);
    }

    public void removeEdge(EntityVariablePair from, EntityVariablePair to) {
        if (from.graphNodeId() == to.graphNodeId()) {
            return;
        }
        if (changed.isEmpty()) {
            graph.startBatchChange();
        }
        var newCount = --counts[from.graphNodeId()][to.graphNodeId()];
        if (newCount == 0) {
            graph.removeEdge(from.graphNodeId(), to.graphNodeId());
        }
        markChanged(to);
    }

    public void markChanged(EntityVariablePair node) {
        if (changed.isEmpty()) {
            graph.startBatchChange();
        }
        changed.set(node.graphNodeId());
    }

    record AffectedEntity(Object entity, VariableUpdaterInfo variableUpdaterInfo) {
        @Override
        public boolean equals(Object o) {
            if (o instanceof AffectedEntity other) {
                return entity == other.entity;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(entity);
        }
    }

    public void updateChanged() {
        graph.endBatchChange();
        var visited = new boolean[instanceList.size()];
        var loopedTracker = new LoopedTracker(visited.length);
        var affectedEntities = Collections.newSetFromMap(new IdentityHashMap<AffectedEntity, Boolean>());
        var nodeHeap = createInitialChangeQueue();

        while (!nodeHeap.isEmpty()) {
            var nextNode = nodeHeap.poll().nodeId;
            if (visited[nextNode]) {
                continue;
            }
            visited[nextNode] = true;
            var shadowVariable = instanceList.get(nextNode);
            var isChanged = updateShadowVariable(shadowVariable,
                    graph.isLooped(loopedTracker, nextNode),
                    affectedEntities);

            if (isChanged) {
                graph.nodeForwardEdges(nextNode).forEachRemaining(
                        (int node) -> {
                            if (!visited[node]) {
                                nodeHeap.add(new AffectedShadowVariable(node, graph.getTopologicalOrder(node)));
                            }
                        });
            }
        }

        updateInvalidityStatusOfAffectedEntities(affectedEntities, loopedTracker);
    }

    @SuppressWarnings("unchecked")
    private boolean updateShadowVariable(EntityVariablePair shadowVariable,
            boolean isLooped,
            Set<AffectedEntity> affectedEntities) {
        var isChanged = false;
        var entity = shadowVariable.entity();
        var shadowVariableReference = shadowVariable.variableReference();
        var oldValue = shadowVariableReference.memberAccessor().executeGetter(entity);

        if (isLooped) {
            // null might be a valid value, and thus it could be the case
            // that is was not looped and null, then turned to looped and null,
            // which is still considered a change.
            isChanged = true;
            affectedEntities.add(new AffectedEntity(entity, shadowVariableReference));
            if (oldValue != null) {
                changedVariableNotifier.beforeVariableChanged().accept(
                        (VariableDescriptor<Solution_>) shadowVariableReference.variableDescriptor(), entity);
                shadowVariableReference.memberAccessor().executeSetter(entity, null);
                changedVariableNotifier.afterVariableChanged().accept(
                        (VariableDescriptor<Solution_>) shadowVariableReference.variableDescriptor(), entity);
            }
        } else {
            var newValue = shadowVariableReference.calculator().apply(entity);

            if (!Objects.equals(oldValue, newValue)) {
                affectedEntities.add(new AffectedEntity(entity, shadowVariableReference));
                changedVariableNotifier.beforeVariableChanged().accept(
                        (VariableDescriptor<Solution_>) shadowVariableReference.variableDescriptor(), entity);
                shadowVariableReference.memberAccessor().executeSetter(entity, newValue);
                changedVariableNotifier.afterVariableChanged().accept(
                        (VariableDescriptor<Solution_>) shadowVariableReference.variableDescriptor(), entity);
                isChanged = true;
            }
        }
        return isChanged;
    }

    record AffectedShadowVariable(int nodeId, int topologicalIndex) implements Comparable<AffectedShadowVariable> {
        @Override
        public int compareTo(AffectedShadowVariable heapItem) {
            return topologicalIndex - heapItem.topologicalIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof AffectedShadowVariable other) {
                return nodeId == other.nodeId;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return nodeId;
        }
    }

    private PriorityQueue<AffectedShadowVariable> createInitialChangeQueue() {
        var heap = new PriorityQueue<AffectedShadowVariable>(instanceList.size());
        // BitSet iteration: get the first set bit at or after 0,
        // then get the first set bit after that bit.
        // Iteration ends when nextSetBit returns -1.
        // This has the potential to overflow, since to do the
        // test, we necessarily need to do nextSetBit(i + 1),
        // and i + 1 can be negative if Integer.MAX_VALUE is set
        // in the BitSet.
        // This should never happen, since arrays in Java are limited
        // to slightly less than Integer.MAX_VALUE.
        for (var i = changed.nextSetBit(0); i >= 0; i = changed.nextSetBit(i + 1)) {
            var topologicalOrder = graph.getTopologicalOrder(i);
            heap.add(new AffectedShadowVariable(i, topologicalOrder));
            if (i == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
        }
        changed.clear();
        return heap;
    }

    @SuppressWarnings("unchecked")
    private void updateInvalidityStatusOfAffectedEntities(Set<AffectedEntity> affectedEntities, LoopedTracker loopedTracker) {
        for (var affectedEntity : affectedEntities) {
            var shadowVariableLoopedDescriptor = affectedEntity.variableUpdaterInfo.shadowVariableLoopedDescriptor();
            if (shadowVariableLoopedDescriptor == null) {
                continue;
            }
            var entity = affectedEntity.entity;
            var isEntityLooped = false;
            for (var node : entityToVariableReferenceMap.get(entity)) {
                if (graph.isLooped(loopedTracker, node.graphNodeId())) {
                    isEntityLooped = true;
                    break;
                }
            }
            var oldValue = shadowVariableLoopedDescriptor.getValue(entity);
            if (!Objects.equals(oldValue, isEntityLooped)) {
                changedVariableNotifier.beforeVariableChanged().accept(
                        (VariableDescriptor<Solution_>) shadowVariableLoopedDescriptor,
                        entity);
                shadowVariableLoopedDescriptor.setValue(entity, isEntityLooped);
                changedVariableNotifier.afterVariableChanged().accept(
                        (VariableDescriptor<Solution_>) shadowVariableLoopedDescriptor,
                        entity);
            }
        }
    }

    public void beforeVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        if (variableReference.entity().type().isInstance(entity)) {
            var updaterList = variableReferenceToBeforeProcessor.getOrDefault(variableReference, Collections.emptyList());
            for (var consumer : updaterList) {
                consumer.accept(this, entity);
            }
        }
    }

    public void afterVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        if (variableReference.entity().type().isInstance(entity)) {
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
