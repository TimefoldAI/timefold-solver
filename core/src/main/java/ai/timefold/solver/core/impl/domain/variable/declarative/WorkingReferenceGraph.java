package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class WorkingReferenceGraph<Solution_> {

    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;

    // These structures are immutable.
    private final Map<Object, List<EntityVariablePair<Solution_>>> entityToVariableReferenceMap;
    private final List<EntityVariablePair<Solution_>> instanceList;
    private final Map<VariableMetaModel<?, ?, ?>, Map<Object, EntityVariablePair<Solution_>>> variableReferenceToInstanceMap;

    // These structures are mutable.
    private final int[][] counts;
    private final TopologicalOrderGraph graph;
    private final BitSet changed;

    // These mutable structures are created once, and reused from there on.
    // Otherwise they were observed being re-created so often
    // that the allocation of arrays would become a major bottleneck.
    // This is made possible by the fact that the instances are only used within {@link #updateChanged()}
    // and do not survive it.
    private final LoopedTracker loopedTracker;
    private final boolean[] visited;

    public WorkingReferenceGraph(VariableReferenceGraph<Solution_> outerGraph,
            IntFunction<TopologicalOrderGraph> graphCreator) {
        changedVariableNotifier = outerGraph.changedVariableNotifier;
        instanceList = List.copyOf(outerGraph.instanceList);
        var instanceCount = instanceList.size();
        // Often the map is a singleton; we improve performance by actually making it so.
        variableReferenceToInstanceMap = Map.copyOf(outerGraph.variableReferenceToInstanceMap);
        counts = new int[instanceCount][instanceCount];
        graph = graphCreator.apply(instanceCount);
        graph.withNodeData(instanceList);
        changed = new BitSet(instanceCount);
        loopedTracker = new LoopedTracker(instanceCount);
        visited = new boolean[instanceCount];

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
        this.entityToVariableReferenceMap = Map.copyOf(entityToVariableReferenceMap);
    }

    public @Nullable EntityVariablePair<Solution_> lookupOrNull(VariableMetaModel<?, ?, ?> variableId, Object entity) {
        return variableReferenceToInstanceMap.getOrDefault(variableId, Collections.emptyMap()).get(entity);
    }

    public void addEdge(@NonNull EntityVariablePair<Solution_> from, @NonNull EntityVariablePair<Solution_> to) {
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

    public void removeEdge(@NonNull EntityVariablePair<Solution_> from, @NonNull EntityVariablePair<Solution_> to) {
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

    public void markChanged(@NonNull EntityVariablePair<Solution_> node) {
        if (changed.isEmpty()) {
            graph.startBatchChange();
        }
        changed.set(node.graphNodeId());
    }

    public void updateChanged() {
        if (changed.isEmpty()) {
            return;
        }
        graph.endBatchChange();
        var affectedEntities = new AffectedEntities<Solution_>(loopedTracker);
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
                    affectedEntities::add);

            if (isChanged) {
                graph.nodeForwardEdges(nextNode).forEachRemaining((int node) -> {
                    if (!visited[node]) {
                        nodeHeap.add(new AffectedShadowVariable(node, graph.getTopologicalOrder(node)));
                    }
                });
            }
        }

        affectedEntities.forEach(this::updateLoopedStatusOfAffectedEntity);
        // Prepare for the next time updateChanged() is called.
        loopedTracker.clear();
        Arrays.fill(visited, false);
    }

    private boolean updateShadowVariable(EntityVariablePair<Solution_> shadowVariable, boolean isLooped,
            BiConsumer<Object, VariableUpdaterInfo<Solution_>> affectedEntityMarker) {
        var entity = shadowVariable.entity();
        var shadowVariableReference = shadowVariable.variableReference();
        var oldValue = shadowVariableReference.memberAccessor().executeGetter(entity);

        if (isLooped) {
            // null might be a valid value, and thus it could be the case
            // that is was not looped and null, then turned to looped and null,
            // which is still considered a change.
            affectedEntityMarker.accept(entity, shadowVariableReference);
            if (oldValue != null) {
                changeShadowVariableAndNotify(shadowVariableReference, entity, null);
            }
            return true;
        } else {
            var newValue = shadowVariableReference.calculator().apply(entity);

            if (!Objects.equals(oldValue, newValue)) {
                affectedEntityMarker.accept(entity, shadowVariableReference);
                changeShadowVariableAndNotify(shadowVariableReference, entity, newValue);
                return true;
            }
        }
        return false;
    }

    private void changeShadowVariableAndNotify(VariableUpdaterInfo<Solution_> shadowVariableReference, Object entity,
            Object newValue) {
        var variableDescriptor = (VariableDescriptor<Solution_>) shadowVariableReference.variableDescriptor();
        changeShadowVariableAndNotify(variableDescriptor, entity, newValue);
    }

    private void changeShadowVariableAndNotify(VariableDescriptor<Solution_> variableDescriptor, Object entity,
            Object newValue) {
        changedVariableNotifier.beforeVariableChanged().accept(variableDescriptor, entity);
        variableDescriptor.setValue(entity, newValue);
        changedVariableNotifier.afterVariableChanged().accept(variableDescriptor, entity);
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

    private void updateLoopedStatusOfAffectedEntity(AffectedEntity<Solution_> affectedEntity, LoopedTracker loopedTracker) {
        var shadowVariableLoopedDescriptor = affectedEntity.variableUpdaterInfo.shadowVariableLoopedDescriptor();
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
            // TODO what if we don't let users treat this as a shadow var?
            //  Nobody can hook up to it => we need not trigger events?
            changeShadowVariableAndNotify(shadowVariableLoopedDescriptor, entity, isEntityLooped);
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

    private static final class AffectedEntities<Solution_> {

        private final Set<AffectedEntity<Solution_>> entitiesForLoopedVarUpdateSet =
                Collections.newSetFromMap(new IdentityHashMap<>());
        private final LoopedTracker loopedTracker;

        public AffectedEntities(LoopedTracker loopedTracker) {
            this.loopedTracker = loopedTracker;
        }

        public void add(Object entity, VariableUpdaterInfo<Solution_> variableUpdaterInfo) {
            if (variableUpdaterInfo.shadowVariableLoopedDescriptor() == null) {
                return;
            }
            entitiesForLoopedVarUpdateSet.add(new AffectedEntity<>(entity, variableUpdaterInfo));
        }

        public void forEach(BiConsumer<AffectedEntity<Solution_>, LoopedTracker> consumer) {
            for (var affectedEntity : entitiesForLoopedVarUpdateSet) {
                consumer.accept(affectedEntity, loopedTracker);
            }
        }

    }

    private record AffectedEntity<Solution_>(Object entity, VariableUpdaterInfo<Solution_> variableUpdaterInfo) {
        @Override
        public boolean equals(Object o) {
            if (o instanceof AffectedEntity<?> other) {
                return entity == other.entity;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(entity);
        }
    }

    private record AffectedShadowVariable(int nodeId, int topologicalIndex)
            implements
                Comparable<AffectedShadowVariable> {
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

}
