package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.Arrays;
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

    // These two fields are stored once, and reused from there on.
    // Otherwise they were observed being re-created so often
    // that the allocation of arrays would become a major bottleneck.
    // This is made possible by the fact that the instances are only used within {@link #updateChanged()}
    // and do not survive it.
    private LoopedTracker loopedTrackerForUpdateChanged;
    private boolean[] visitedForUpdateChanged;

    public VariableReferenceGraph(ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        this.changedVariableNotifier = changedVariableNotifier;
        variableReferenceToInstanceMap = new HashMap<>();
        instanceList = new ArrayList<>();
        variableReferenceToBeforeProcessor = new HashMap<>();
        variableReferenceToAfterProcessor = new HashMap<>();
        fixedEdges = new HashMap<>();
        entityToVariableReferenceMap = new IdentityHashMap<>();
    }

    public <Entity_> EntityVariablePair addVariableReferenceEntity(Entity_ entity, VariableUpdaterInfo variableReference) {
        var variableId = variableReference.id();
        var instanceMap = variableReferenceToInstanceMap.get(variableId);
        var instance = instanceMap == null ? null : instanceMap.get(entity);
        if (instance != null) {
            return instance;
        }
        if (instanceMap == null) {
            instanceMap = new IdentityHashMap<>();
            variableReferenceToInstanceMap.put(variableId, instanceMap);
        }
        var node = new EntityVariablePair(entity, variableReference, instanceList.size());
        instanceMap.put(entity, node);
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
        var instanceCount = instanceList.size();
        counts = new int[instanceCount][instanceCount];
        graph = graphCreator.apply(instanceCount);
        graph.withNodeData(instanceList);
        changed = new BitSet(instanceCount);
        loopedTrackerForUpdateChanged = new LoopedTracker(instanceCount);
        visitedForUpdateChanged = new boolean[instanceCount];

        graph.startBatchChange();
        var visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var instance : instanceList) {
            var entity = instance.entity();
            if (visited.add(entity)) {
                for (var variableId : variableReferenceToAfterProcessor.keySet()) {
                    afterVariableChanged(variableId, entity);
                }
            }
            entityToVariableReferenceMap.computeIfAbsent(entity, ignored -> new ArrayList<>())
                    .add(instance);
        }
        for (var fixedEdgeEntry : fixedEdges.entrySet()) {
            for (var toEdge : fixedEdgeEntry.getValue()) {
                addEdge(fixedEdgeEntry.getKey(), toEdge);
            }
        }
    }

    public @Nullable EntityVariablePair lookupOrNull(VariableMetaModel<?, ?, ?> variableId, Object entity) {
        return variableReferenceToInstanceMap.getOrDefault(variableId, Collections.emptyMap()).get(entity);
    }

    public @NonNull EntityVariablePair lookupOrError(VariableMetaModel<?, ?, ?> variableId, Object entity) {
        var out = lookupOrNull(variableId, entity);
        if (out == null) {
            throw new IllegalArgumentException();
        }
        return out;
    }

    public void addFixedEdge(@NonNull EntityVariablePair from, @NonNull EntityVariablePair to) {
        if (from.graphNodeId() == to.graphNodeId()) {
            return;
        }
        fixedEdges.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    }

    public void addEdge(@NonNull EntityVariablePair from, @NonNull EntityVariablePair to) {
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

    public void removeEdge(@NonNull EntityVariablePair from, @NonNull EntityVariablePair to) {
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

    public void markChanged(@NonNull EntityVariablePair node) {
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
        var affectedEntities = new AffectedEntities(loopedTrackerForUpdateChanged);
        var nodeHeap = createInitialChangeQueue();

        while (!nodeHeap.isEmpty()) {
            var nextNode = nodeHeap.poll().nodeId;
            if (visitedForUpdateChanged[nextNode]) {
                continue;
            }
            visitedForUpdateChanged[nextNode] = true;
            var shadowVariable = instanceList.get(nextNode);
            var isChanged = updateShadowVariable(shadowVariable,
                    graph.isLooped(loopedTrackerForUpdateChanged, nextNode),
                    affectedEntities::add);

            if (isChanged) {
                graph.nodeForwardEdges(nextNode).forEachRemaining((int node) -> {
                    if (!visitedForUpdateChanged[node]) {
                        nodeHeap.add(new AffectedShadowVariable(node, graph.getTopologicalOrder(node)));
                    }
                });
            }
        }

        affectedEntities.forEach(this::updateLoopedStatusOfAffectedEntity);
        // Prepare for the next time updateChanged() is called.
        loopedTrackerForUpdateChanged.clear();
        Arrays.fill(visitedForUpdateChanged, false);
    }

    private boolean updateShadowVariable(EntityVariablePair shadowVariable, boolean isLooped,
            BiConsumer<Object, VariableUpdaterInfo> affectedEntityMarker) {
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

    @SuppressWarnings("unchecked")
    private void changeShadowVariableAndNotify(VariableUpdaterInfo shadowVariableReference, Object entity, Object newValue) {
        var variableDescriptor = (VariableDescriptor<Solution_>) shadowVariableReference.variableDescriptor();
        changedVariableNotifier.beforeVariableChanged().accept(variableDescriptor, entity);
        shadowVariableReference.memberAccessor().executeSetter(entity, newValue);
        changedVariableNotifier.afterVariableChanged().accept(variableDescriptor, entity);
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

    private void updateLoopedStatusOfAffectedEntity(AffectedEntity affectedEntity, LoopedTracker loopedTracker) {
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
            changedVariableNotifier.beforeVariableChanged().accept(
                    (VariableDescriptor<Solution_>) shadowVariableLoopedDescriptor,
                    entity);
            shadowVariableLoopedDescriptor.setValue(entity, isEntityLooped);
            changedVariableNotifier.afterVariableChanged().accept(
                    (VariableDescriptor<Solution_>) shadowVariableLoopedDescriptor,
                    entity);
        }
    }

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

    private static final class AffectedEntities {

        private final Set<AffectedEntity> entitiesForLoopedVarUpdateSet = Collections.newSetFromMap(new IdentityHashMap<>());
        private final LoopedTracker loopedTracker;

        public AffectedEntities(LoopedTracker loopedTracker) {
            this.loopedTracker = loopedTracker;
        }

        public void add(Object entity, VariableUpdaterInfo variableUpdaterInfo) {
            if (variableUpdaterInfo.shadowVariableLoopedDescriptor() == null) {
                return;
            }
            entitiesForLoopedVarUpdateSet.add(new AffectedEntity(entity, variableUpdaterInfo));
        }

        public void forEach(BiConsumer<AffectedEntity, LoopedTracker> consumer) {
            for (var affectedEntity : entitiesForLoopedVarUpdateSet) {
                consumer.accept(affectedEntity, loopedTracker);
            }
        }

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

}
