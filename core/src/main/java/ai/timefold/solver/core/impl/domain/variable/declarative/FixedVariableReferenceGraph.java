package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Spliterators;
import java.util.function.IntFunction;
import java.util.stream.StreamSupport;

import ai.timefold.solver.core.api.score.analysis.VariableLoop;

import org.jspecify.annotations.NonNull;

public final class FixedVariableReferenceGraph<Solution_>
        extends AbstractVariableReferenceGraph<Solution_, PriorityQueue<BaseTopologicalOrderGraph.NodeTopologicalOrder>> {
    // These are immutable
    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;
    private final BitSet isChanged;
    private final int[][] cachedComponentForwardEdges;
    // These are mutable
    private boolean isFinalized = false;

    public FixedVariableReferenceGraph(VariableReferenceGraphBuilder<Solution_> outerGraph,
            IntFunction<TopologicalOrderGraph> graphCreator) {
        super(outerGraph, graphCreator);
        isChanged = new BitSet(nodeList.size());
        cachedComponentForwardEdges = new int[nodeList.size()][];
        graph.commitChanges(isChanged);
        isChanged.clear();
        isFinalized = true;

        // Now that we know the topological order of nodes, add
        // each node to changed.
        changedVariableNotifier = outerGraph.changedVariableNotifier;
        for (var node = 0; node < nodeList.size(); node++) {
            var finalNode = node;
            cachedComponentForwardEdges[node] = StreamSupport
                    .intStream(() -> Spliterators.spliterator(graph.nodeForwardEdges(finalNode), 0, 0),
                            0, false)
                    .toArray();
            changeTracker.add(nodeTopologicalOrders[node]);
            var variableReference = nodeList.get(node).variableReferences().get(0);
            var entityConsistencyState = variableReference.entityConsistencyState();
            if (variableReference.groupEntities() != null) {
                for (var groupEntity : variableReference.groupEntities()) {
                    entityConsistencyState.setEntityIsInconsistent(changedVariableNotifier, groupEntity,
                            false);
                }
            } else {
                for (var shadowEntity : outerGraph.entityToEntityId.keySet()) {
                    if (variableReference.id().entity().type()
                            .isInstance(shadowEntity)) {
                        entityConsistencyState.setEntityIsInconsistent(changedVariableNotifier,
                                shadowEntity, false);
                    }
                }
            }
        }
    }

    @Override
    protected PriorityQueue<BaseTopologicalOrderGraph.NodeTopologicalOrder> createChangeTracker(int instanceCount) {
        return new PriorityQueue<>(instanceCount);
    }

    @Override
    void markChanged(@NonNull GraphNode<Solution_> node) {
        // Before the graph is finalized, ignore changes, since
        // we don't know the topological order yet
        if (isFinalized) {
            var nodeId = node.graphNodeId();
            if (!isChanged.get(nodeId)) {
                changeTracker.add(nodeTopologicalOrders[nodeId]);
                isChanged.set(nodeId);
            }
        }
    }

    @Override
    boolean innerUpdateChanged() {
        // The nodes are polled in topological order, so every node an update can reach is polled
        // after it: updating each node once is enough, however many times it was queued.
        var updated = new BitSet(nodeList.size());

        // NOTE: This assumes the user did not add any fixed loops to
        // their graph (i.e. have two variables ALWAYS depend on one-another).
        while (!changeTracker.isEmpty()) {
            var changedNodeId = changeTracker.poll().nodeId();
            if (updated.get(changedNodeId)) {
                continue;
            }
            updated.set(changedNodeId);
            var entityVariable = nodeList.get(changedNodeId);
            var entity = entityVariable.entity();
            var shadowVariableReferences = entityVariable.variableReferences();
            for (var shadowVariableReference : shadowVariableReferences) {
                var isVariableChanged = shadowVariableReference.update(entity, false, changedVariableNotifier);
                if (isVariableChanged) {
                    for (var nextNode : cachedComponentForwardEdges[changedNodeId]) {
                        if (!updated.get(nextNode)) {
                            changeTracker.add(nodeTopologicalOrders[nextNode]);
                        }
                    }
                }
            }
        }
        isChanged.clear();
        return true;
    }

    @Override
    public List<VariableLoop> getVariableLoops() {
        return Collections.emptyList();
    }
}
