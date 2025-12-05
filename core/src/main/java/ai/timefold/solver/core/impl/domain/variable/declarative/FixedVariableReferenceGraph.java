package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.BitSet;
import java.util.PriorityQueue;
import java.util.function.IntFunction;

import org.jspecify.annotations.NonNull;

public final class FixedVariableReferenceGraph<Solution_>
        extends AbstractVariableReferenceGraph<Solution_, PriorityQueue<BaseTopologicalOrderGraph.NodeTopologicalOrder>> {
    // These are immutable
    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;

    // These are mutable
    private boolean isFinalized = false;

    public FixedVariableReferenceGraph(VariableReferenceGraphBuilder<Solution_> outerGraph,
            IntFunction<TopologicalOrderGraph> graphCreator) {
        super(outerGraph, graphCreator);
        // We don't use a bit set to store changes, so pass a one-use instance
        graph.commitChanges(new BitSet(nodeList.size()));
        isFinalized = true;

        // Now that we know the topological order of nodes, add
        // each node to changed.
        changedVariableNotifier = outerGraph.changedVariableNotifier;
        for (var node = 0; node < nodeList.size(); node++) {
            changeSet.add(nodeTopologicalOrders[node]);
            var variableReference = nodeList.get(node).variableReferences().get(0);
            var entityConsistencyState = variableReference.entityConsistencyState();
            if (variableReference.groupEntities() != null) {
                for (var groupEntity : variableReference.groupEntities()) {
                    entityConsistencyState.setEntityIsInconsistent(changedVariableNotifier, groupEntity,
                            false);
                }
            } else {
                for (var shadowEntity : outerGraph.entityToEntityId.keySet()) {
                    if (variableReference.variableDescriptor().getEntityDescriptor().getEntityClass()
                            .isInstance(shadowEntity)) {
                        entityConsistencyState.setEntityIsInconsistent(changedVariableNotifier,
                                shadowEntity, false);
                    }
                }
            }
        }
    }

    @Override
    protected PriorityQueue<BaseTopologicalOrderGraph.NodeTopologicalOrder> createChangeSet(int instanceCount) {
        return new PriorityQueue<>(instanceCount);
    }

    @Override
    public void markChanged(@NonNull GraphNode<Solution_> node) {
        // Before the graph is finalized, ignore changes, since
        // we don't know the topological order yet
        if (isFinalized) {
            changeSet.add(nodeTopologicalOrders[node.graphNodeId()]);
        }
    }

    @Override
    public void updateChanged() {
        BitSet visited;
        if (!changeSet.isEmpty()) {
            visited = new BitSet(nodeList.size());
            visited.set(changeSet.peek().nodeId());
        } else {
            return;
        }

        // NOTE: This assumes the user did not add any fixed loops to
        // their graph (i.e. have two variables ALWAYS depend on one-another).
        while (!changeSet.isEmpty()) {
            var changedNode = changeSet.poll();
            var entityVariable = nodeList.get(changedNode.nodeId());
            var entity = entityVariable.entity();
            var shadowVariableReferences = entityVariable.variableReferences();
            for (var shadowVariableReference : shadowVariableReferences) {
                var isVariableChanged = shadowVariableReference.updateIfChanged(entity, changedVariableNotifier);
                if (isVariableChanged) {
                    for (var iterator = graph.nodeForwardEdges(changedNode.nodeId()); iterator.hasNext();) {
                        var nextNode = iterator.next();
                        if (visited.get(nextNode)) {
                            continue;
                        }
                        visited.set(nextNode);
                        changeSet.add(nodeTopologicalOrders[nextNode]);
                    }
                }
            }
        }
    }
}
