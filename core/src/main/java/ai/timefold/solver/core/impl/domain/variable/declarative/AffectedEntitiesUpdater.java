package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.util.LinkedIdentityHashSet;

final class AffectedEntitiesUpdater<Solution_>
        implements Consumer<BitSet> {

    // From WorkingReferenceGraph.
    private final BaseTopologicalOrderGraph graph;
    private final List<EntityVariablePair<Solution_>> instanceList; // Immutable.
    private final Function<Object, List<EntityVariablePair<Solution_>>> entityVariablePairFunction;
    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;

    // Internal state; expensive to create, therefore we reuse.
    private final AffectedEntities<Solution_> affectedEntities;
    private final LoopedTracker loopedTracker;
    private final BitSet visited;
    private final PriorityQueue<BaseTopologicalOrderGraph.NodeTopologicalOrder> changeQueue;

    AffectedEntitiesUpdater(BaseTopologicalOrderGraph graph, List<EntityVariablePair<Solution_>> instanceList,
            Function<Object, List<EntityVariablePair<Solution_>>> entityVariablePairFunction,
            ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        this.graph = graph;
        this.instanceList = instanceList;
        this.entityVariablePairFunction = entityVariablePairFunction;
        this.changedVariableNotifier = changedVariableNotifier;
        var instanceCount = instanceList.size();
        this.affectedEntities = new AffectedEntities<>(this::updateLoopedStatusOfAffectedEntity);
        this.loopedTracker = new LoopedTracker(instanceCount);
        this.visited = new BitSet(instanceCount);
        this.changeQueue = new PriorityQueue<>(instanceCount);
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

        affectedEntities.processAndClear();
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

    private void updateLoopedStatusOfAffectedEntity(Object affectedEntity) {
        ShadowVariableLoopedVariableDescriptor<Solution_> shadowVariableLoopedDescriptor = null;
        var isEntityLooped = false;
        for (var node : entityVariablePairFunction.apply(affectedEntity)) {
            // All variables come from the same entity,
            // therefore all have the same looped marker.
            shadowVariableLoopedDescriptor = node.variableReferences().get(0).shadowVariableLoopedDescriptor();
            if (graph.isLooped(loopedTracker, node.graphNodeId())) {
                isEntityLooped = true;
                break;
            }
        }
        if (shadowVariableLoopedDescriptor == null) {
            // At this point, affectedEntity is guaranteed to have looped marker.
            // Otherwise AffectedEntities would not have sent it here.
            throw new IllegalStateException("Impossible state: loop marker descriptor does not exist.");
        }
        var oldValue = shadowVariableLoopedDescriptor.getValue(affectedEntity);
        if (!Objects.equals(oldValue, isEntityLooped)) {
            changeShadowVariableAndNotify(shadowVariableLoopedDescriptor, affectedEntity, isEntityLooped);
        }

    }

    private boolean updateEntityShadowVariables(EntityVariablePair<Solution_> entityVariable, boolean isLooped) {
        var entity = entityVariable.entity();
        var shadowVariableReferences = entityVariable.variableReferences();
        var loopDescriptor = shadowVariableReferences.get(0).shadowVariableLoopedDescriptor();
        var anyChanged = false;

        if (loopDescriptor != null) {
            var oldLooped = loopDescriptor.getValue(entity);
            if (!Objects.equals(oldLooped, isLooped)) {
                // Loop status change; add to affected entities
                affectedEntities.add(entityVariable);
                anyChanged = true;
            }
        }

        for (var shadowVariableReference : shadowVariableReferences) {
            anyChanged |= updateShadowVariable(entityVariable, isLooped, shadowVariableReference, entity);
        }

        return anyChanged;
    }

    private boolean updateShadowVariable(EntityVariablePair<Solution_> entityVariable, boolean isLooped,
            VariableUpdaterInfo<Solution_> shadowVariableReference, Object entity) {
        var oldValue = shadowVariableReference.memberAccessor().executeGetter(entity);
        if (isLooped) {
            if (oldValue != null) {
                affectedEntities.add(entityVariable);
                changeShadowVariableAndNotify(shadowVariableReference, entity, null);
            }
            return true;
        } else {
            var newValue = shadowVariableReference.calculator().apply(entity);
            if (!Objects.equals(oldValue, newValue)) {
                affectedEntities.add(entityVariable);
                changeShadowVariableAndNotify(shadowVariableReference, entity, newValue);
                return true;
            }
        }
        return false;
    }

    private void changeShadowVariableAndNotify(VariableUpdaterInfo<Solution_> shadowVariableReference, Object entity,
            Object newValue) {
        var variableDescriptor = shadowVariableReference.variableDescriptor();
        changeShadowVariableAndNotify(variableDescriptor, entity, newValue);
    }

    private void changeShadowVariableAndNotify(VariableDescriptor<Solution_> variableDescriptor, Object entity,
            Object newValue) {
        changedVariableNotifier.beforeVariableChanged().accept(variableDescriptor, entity);
        variableDescriptor.setValue(entity, newValue);
        changedVariableNotifier.afterVariableChanged().accept(variableDescriptor, entity);
    }

    private static final class AffectedEntities<Solution_> {

        private final Consumer<Object> consumer;
        private final Set<Object> entitiesForLoopedVarUpdateSet;

        public AffectedEntities(Consumer<Object> consumer) {
            this.consumer = consumer;
            this.entitiesForLoopedVarUpdateSet = new LinkedIdentityHashSet<>();
        }

        public void add(EntityVariablePair<Solution_> shadowVariable) {
            var shadowVariableLoopedDescriptor = shadowVariable.variableReferences().get(0).shadowVariableLoopedDescriptor();
            if (shadowVariableLoopedDescriptor == null) {
                return;
            }
            entitiesForLoopedVarUpdateSet.add(shadowVariable.entity());
        }

        public void processAndClear() {
            for (var entity : entitiesForLoopedVarUpdateSet) {
                consumer.accept(entity);
            }
            entitiesForLoopedVarUpdateSet.clear();
        }

    }

}
