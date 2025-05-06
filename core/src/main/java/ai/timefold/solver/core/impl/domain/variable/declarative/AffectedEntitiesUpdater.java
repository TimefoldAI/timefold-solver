package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

final class AffectedEntitiesUpdater<Solution_>
        implements Consumer<BitSet> {

    // From WorkingReferenceGraph.
    private final BaseTopologicalOrderGraph graph;
    private final List<EntityVariablePair<Solution_>> instanceList; // Immutable.
    private final Function<Object, List<EntityVariablePair<Solution_>>> entityVariablePairFunction;
    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;

    // Internal state; expensive to create, therefore we reuse.
    private final LoopedTracker loopedTracker;
    private final boolean[] visited;
    private final PriorityQueue<AffectedShadowVariable> changeQueue;

    AffectedEntitiesUpdater(BaseTopologicalOrderGraph graph, List<EntityVariablePair<Solution_>> instanceList,
            Function<Object, List<EntityVariablePair<Solution_>>> entityVariablePairFunction,
            ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        this.graph = graph;
        this.instanceList = instanceList;
        this.entityVariablePairFunction = entityVariablePairFunction;
        this.changedVariableNotifier = changedVariableNotifier;
        var instanceCount = instanceList.size();
        this.loopedTracker = new LoopedTracker(instanceCount);
        this.visited = new boolean[instanceCount];
        this.changeQueue = new PriorityQueue<>(instanceCount);
    }

    @Override
    public void accept(BitSet changed) {
        var affectedEntities = new AffectedEntities<Solution_>(loopedTracker);
        initializeChangeQueue(changed);

        while (!changeQueue.isEmpty()) {
            var nextNode = changeQueue.poll().nodeId;
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
                        changeQueue.add(new AffectedShadowVariable(node, graph.getTopologicalOrder(node)));
                    }
                });
            }
        }

        affectedEntities.forEach(this::updateLoopedStatusOfAffectedEntity);
        // Prepare for the next time updateChanged() is called.
        // No need to clear changeQueue, as that already finishes empty.
        loopedTracker.clear();
        Arrays.fill(visited, false);
    }

    private void initializeChangeQueue(BitSet changed) {
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
            changeQueue.add(new AffectedShadowVariable(i, topologicalOrder));
            if (i == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
        }
        // TODO why can this not be someplace after here?
        //  attempts to move the clearing of this bitset to a more prominent place
        //  have resulted in failing tests, for no obvious reason.
        changed.clear();
    }

    private void updateLoopedStatusOfAffectedEntity(AffectedEntity<Solution_> affectedEntity, LoopedTracker loopedTracker) {
        var shadowVariableLoopedDescriptor = affectedEntity.variableUpdaterInfo.shadowVariableLoopedDescriptor();
        var entity = affectedEntity.entity;
        var isEntityLooped = false;
        for (var node : entityVariablePairFunction.apply(entity)) {
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
