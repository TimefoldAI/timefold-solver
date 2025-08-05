package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

final class AffectedEntitiesUpdater<Solution_>
        implements Consumer<BitSet> {

    // From WorkingReferenceGraph.
    private final BaseTopologicalOrderGraph graph;
    private final List<EntityVariablePair<Solution_>> instanceList; // Immutable.
    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;

    // Internal state; expensive to create, therefore we reuse.
    private final LoopedTracker loopedTracker;
    private final BitSet visited;
    private final PriorityQueue<BaseTopologicalOrderGraph.NodeTopologicalOrder> changeQueue;

    AffectedEntitiesUpdater(BaseTopologicalOrderGraph graph, List<EntityVariablePair<Solution_>> instanceList,
            Function<Object, List<EntityVariablePair<Solution_>>> entityVariablePairFunction,
            int entityCount, ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        this.graph = graph;
        this.instanceList = instanceList;
        this.changedVariableNotifier = changedVariableNotifier;
        var instanceCount = instanceList.size();
        this.loopedTracker = new LoopedTracker(instanceCount,
                createNodeToEntityNodes(entityCount, instanceList, entityVariablePairFunction));
        this.visited = new BitSet(instanceCount);
        this.changeQueue = new PriorityQueue<>(instanceCount);
    }

    static <Solution_> int[][] createNodeToEntityNodes(int entityCount,
            List<EntityVariablePair<Solution_>> instanceList,
            Function<Object, List<EntityVariablePair<Solution_>>> entityVariablePairFunction) {
        record EntityIdPair(Object entity, int entityId) {
            @Override
            public boolean equals(Object o) {
                if (!(o instanceof EntityIdPair that))
                    return false;
                return entityId == that.entityId;
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(entityId);
            }
        }
        int[][] out = new int[entityCount][];
        var entityToNodes = new IdentityHashMap<Integer, int[]>();
        var entityIdPairSet = instanceList.stream()
                .map(node -> new EntityIdPair(node.entity(), node.entityId()))
                .collect(Collectors.toSet());
        for (var entityIdPair : entityIdPairSet) {
            entityToNodes.put(entityIdPair.entityId(),
                    entityVariablePairFunction.apply(entityIdPair.entity).stream().mapToInt(EntityVariablePair::graphNodeId)
                            .toArray());
        }

        for (var entry : entityToNodes.entrySet()) {
            out[entry.getKey()] = entry.getValue();
        }

        return out;
    }

    @Override
    public void accept(BitSet changed) {
        initializeChangeQueue(changed);

        while (!changeQueue.isEmpty()) {
            var nextNode = changeQueue.poll().nodeId();
            if (visited.get(nextNode)) {
                continue;
            }
            visited.set(nextNode);
            var shadowVariable = instanceList.get(nextNode);
            var isChanged = updateEntityShadowVariables(shadowVariable, graph.isLooped(loopedTracker, nextNode));

            if (isChanged) {
                var iterator = graph.nodeForwardEdges(nextNode);
                while (iterator.hasNext()) {
                    var nextNodeForwardEdge = iterator.nextInt();
                    if (!visited.get(nextNodeForwardEdge)) {
                        changeQueue.add(graph.getTopologicalOrder(nextNodeForwardEdge));
                    }
                }
            }
        }

        // Prepare for the next time updateChanged() is called.
        // No need to clear changeQueue, as that already finishes empty.
        loopedTracker.clear();
        visited.clear();
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
            changeQueue.add(graph.getTopologicalOrder(i));
            if (i == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
        }
        changed.clear();
    }

    private boolean updateEntityShadowVariables(EntityVariablePair<Solution_> entityVariable, boolean isVariableLooped) {
        var entity = entityVariable.entity();
        var shadowVariableReferences = entityVariable.variableReferences();
        var loopDescriptor = shadowVariableReferences.get(0).shadowVariableLoopedDescriptor();
        var anyChanged = false;

        if (loopDescriptor != null) {
            // Do not need to update anyChanged here; the graph already marked
            // all nodes whose looped status changed for us
            var groupEntities = shadowVariableReferences.get(0).groupEntities();
            var groupEntityIds = entityVariable.groupEntityIds();

            if (groupEntities != null) {
                for (var i = 0; i < groupEntityIds.length; i++) {
                    var groupEntity = groupEntities[i];
                    var groupEntityId = groupEntityIds[i];
                    updateLoopedStatusOfEntity(groupEntity, groupEntityId, loopDescriptor);
                }
            } else {
                updateLoopedStatusOfEntity(entity, entityVariable.entityId(), loopDescriptor);
            }
        }

        for (var shadowVariableReference : shadowVariableReferences) {
            anyChanged |= updateShadowVariable(isVariableLooped, shadowVariableReference, entity);
        }

        return anyChanged;
    }

    private void updateLoopedStatusOfEntity(Object entity, int entityId,
            ShadowVariableLoopedVariableDescriptor<Solution_> loopDescriptor) {
        var isEntityLooped = loopedTracker.isEntityLooped(graph, entityId);
        var oldLooped = loopDescriptor.getValue(entity);
        if (!Objects.equals(oldLooped, isEntityLooped)) {
            changeShadowVariableAndNotify(loopDescriptor, entity, isEntityLooped);
        }
    }

    private boolean updateShadowVariable(boolean isLooped,
            VariableUpdaterInfo<Solution_> shadowVariableReference, Object entity) {
        if (isLooped) {
            return shadowVariableReference.updateIfChanged(entity, null, changedVariableNotifier);
        } else {
            return shadowVariableReference.updateIfChanged(entity, changedVariableNotifier);
        }
    }

    private void changeShadowVariableAndNotify(VariableDescriptor<Solution_> variableDescriptor, Object entity,
            Object newValue) {
        changedVariableNotifier.beforeVariableChanged().accept(variableDescriptor, entity);
        variableDescriptor.setValue(entity, newValue);
        changedVariableNotifier.afterVariableChanged().accept(variableDescriptor, entity);
    }
}
