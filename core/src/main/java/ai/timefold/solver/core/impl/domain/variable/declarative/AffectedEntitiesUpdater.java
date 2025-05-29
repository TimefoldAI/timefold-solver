package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.function.Consumer;

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
            ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        this.graph = graph;
        this.instanceList = instanceList;
        this.changedVariableNotifier = changedVariableNotifier;
        var instanceCount = instanceList.size();
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
            var isChanged = updateShadowVariable(shadowVariable, graph.isLooped(loopedTracker, nextNode));

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

    private boolean updateShadowVariable(EntityVariablePair<Solution_> entityVariable, boolean isLooped) {
        var entity = entityVariable.entity();
        var shadowVariableReference = entityVariable.variableReference();
        var oldValue = shadowVariableReference.memberAccessor().executeGetter(entity);
        var loopDescriptor = shadowVariableReference.shadowVariableLoopedDescriptor();
        if (loopDescriptor != null) {
            var oldLooped = (boolean) loopDescriptor.getValue(entity);
            if (oldLooped != isLooped) {
                // Loop status change; mark it
                changeShadowVariableAndNotify(loopDescriptor, entity, isLooped);
            }
        }

        if (isLooped) {
            if (oldValue != null) {
                changeShadowVariableAndNotify(shadowVariableReference, entity, null);
            }
            return true;
        } else {
            var newValue = shadowVariableReference.calculator().apply(entity);
            if (!Objects.equals(oldValue, newValue)) {
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

}
