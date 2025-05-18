package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiConsumer;
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
    private final boolean[] visited;
    private final PriorityQueue<BaseTopologicalOrderGraph.NodeTopologicalOrder> changeQueue;

    @SuppressWarnings("unchecked")
    AffectedEntitiesUpdater(BaseTopologicalOrderGraph graph, List<EntityVariablePair<Solution_>> instanceList,
            Function<Object, List<EntityVariablePair<Solution_>>> entityVariablePairFunction,
            ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        this.graph = graph;
        this.instanceList = instanceList;
        this.entityVariablePairFunction = entityVariablePairFunction;
        this.changedVariableNotifier = changedVariableNotifier;
        var instanceCount = instanceList.size();
        this.affectedEntities = new AffectedEntities<>(this::updateLoopedStatusOfAffectedEntity, instanceList.stream()
                .map(e -> e.variableReference().shadowVariableLoopedDescriptor())
                .filter(Objects::nonNull)
                .distinct()
                .toArray(ShadowVariableLoopedVariableDescriptor[]::new));
        this.loopedTracker = new LoopedTracker(instanceCount);
        this.visited = new boolean[instanceCount];
        this.changeQueue = new PriorityQueue<>(instanceCount);
    }

    @Override
    public void accept(BitSet changed) {
        initializeChangeQueue(changed);

        while (!changeQueue.isEmpty()) {
            var nextNode = changeQueue.poll().nodeId();
            if (visited[nextNode]) {
                continue;
            }
            visited[nextNode] = true;
            var shadowVariable = instanceList.get(nextNode);
            var isChanged = updateShadowVariable(shadowVariable, graph.isLooped(loopedTracker, nextNode));

            if (isChanged) {
                var iterator = graph.nodeForwardEdges(nextNode);
                while (iterator.hasNext()) {
                    var nextNodeForwardEdge = iterator.nextInt();
                    if (!visited[nextNodeForwardEdge]) {
                        changeQueue.add(graph.getTopologicalOrder(nextNodeForwardEdge));
                    }
                }
            }
        }

        affectedEntities.processAndClear();
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
            changeQueue.add(graph.getTopologicalOrder(i));
            if (i == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
        }
        // TODO why can this not be someplace after here?
        //  attempts to move the clearing of this bitset to a more prominent place
        //  have resulted in failing tests, for no obvious reason.
        changed.clear();
    }

    private void updateLoopedStatusOfAffectedEntity(Object affectedEntity,
            ShadowVariableLoopedVariableDescriptor<Solution_> shadowVariableLoopedDescriptor) {
        var isEntityLooped = false;
        for (var node : entityVariablePairFunction.apply(affectedEntity)) {
            if (graph.isLooped(loopedTracker, node.graphNodeId())) {
                isEntityLooped = true;
                break;
            }
        }
        var oldValue = shadowVariableLoopedDescriptor.getValue(affectedEntity);
        if (!Objects.equals(oldValue, isEntityLooped)) {
            // TODO what if we don't let users treat this as a shadow var?
            //  Nobody can hook up to it => we need not trigger events?
            changeShadowVariableAndNotify(shadowVariableLoopedDescriptor, affectedEntity, isEntityLooped);
        }

    }

    private boolean updateShadowVariable(EntityVariablePair<Solution_> shadowVariable, boolean isLooped) {
        var entity = shadowVariable.entity();
        var shadowVariableReference = shadowVariable.variableReference();
        var shadowVariableLoopedDescriptor = shadowVariableReference.shadowVariableLoopedDescriptor();
        var oldValue = shadowVariableReference.memberAccessor().executeGetter(entity);

        if (isLooped) {
            // null might be a valid value, and thus it could be the case
            // that is was not looped and null, then turned to looped and null,
            // which is still considered a change.
            affectedEntities.add(entity, shadowVariableLoopedDescriptor);
            if (oldValue != null) {
                changeShadowVariableAndNotify(shadowVariableReference, entity, null);
            }
            return true;
        } else {
            var newValue = shadowVariableReference.calculator().apply(entity);

            if (!Objects.equals(oldValue, newValue)) {
                affectedEntities.add(entity, shadowVariableLoopedDescriptor);
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

        private final BiConsumer<Object, ShadowVariableLoopedVariableDescriptor<Solution_>> consumer;
        private final Map<ShadowVariableLoopedVariableDescriptor<Solution_>, Set<Object>> entitiesForLoopedVarUpdateSet;

        @SuppressWarnings("unchecked")
        public AffectedEntities(BiConsumer<Object, ShadowVariableLoopedVariableDescriptor<Solution_>> consumer,
                ShadowVariableLoopedVariableDescriptor<Solution_>... shadowVariableLoopedDescriptors) {
            this.consumer = consumer;

            var entryList = new ArrayList<Map.Entry<ShadowVariableLoopedVariableDescriptor<Solution_>, Set<Object>>>();
            for (var shadowVariableLoopedDescriptor : shadowVariableLoopedDescriptors) {
                entryList.add(Map.entry(shadowVariableLoopedDescriptor, new LinkedIdentityHashSet<>()));
            }
            this.entitiesForLoopedVarUpdateSet = Map.ofEntries(entryList.toArray(new Map.Entry[0]));
        }

        public void add(Object entity, ShadowVariableLoopedVariableDescriptor<Solution_> shadowVariableLoopedDescriptor) {
            if (shadowVariableLoopedDescriptor == null) {
                return;
            }
            entitiesForLoopedVarUpdateSet.get(shadowVariableLoopedDescriptor).add(entity);
        }

        public void processAndClear() {
            for (var affectedEntitiesPerDescriptor : entitiesForLoopedVarUpdateSet.entrySet()) {
                var affectedEntitySet = affectedEntitiesPerDescriptor.getValue();
                for (var affectedEntity : affectedEntitySet) {
                    consumer.accept(affectedEntity, affectedEntitiesPerDescriptor.getKey());
                }
                affectedEntitySet.clear(); // Keep the set, to not have to recreate and resize later.
            }
        }

    }

}
