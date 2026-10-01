package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Spliterators;
import java.util.function.IntFunction;
import java.util.stream.StreamSupport;

import ai.timefold.solver.core.api.score.analysis.VariableLoop;

import org.jspecify.annotations.NonNull;

public final class FixedVariableReferenceGraph<Solution_>
        extends AbstractVariableReferenceGraph<Solution_, NodeTopologicalOrderQueue> {
    // These are immutable
    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;
    private final int[][] cachedComponentForwardEdges;
    // These are mutable
    private boolean isFinalized = false;

    public FixedVariableReferenceGraph(VariableReferenceGraphBuilder<Solution_> outerGraph,
            IntFunction<TopologicalOrderGraph> graphCreator) {
        super(outerGraph, graphCreator);
        cachedComponentForwardEdges = new int[nodeList.size()][];
        graph.commitChanges(new BitSet(nodeList.size()));
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
            markChanged(nodeList.get(node));
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
    protected NodeTopologicalOrderQueue createChangeTracker(int instanceCount) {
        return new NodeTopologicalOrderQueue(graph, instanceCount);
    }

    @Override
    void markChanged(@NonNull GraphNode<Solution_> node) {
        // Before the graph is finalized, ignore changes, since
        // we don't know the topological order yet
        if (isFinalized) {
            changeTracker.offer(node.graphNodeId());
        }
    }

    @Override
    boolean innerUpdateChanged() {
        // A fixed graph is acyclic - assertNoFixedLoops() rejects a looped one at build time -
        // and no edge is added or removed afterwards, so every edge runs strictly forward in
        // topological order. The queue polls in that order and drops a node already in it,
        // so each node is updated at most once per pass.
        while (!changeTracker.isEmpty()) {
            var changedNodeId = changeTracker.poll();
            var entityVariable = nodeList.get(changedNodeId);
            var entity = entityVariable.entity();
            var shadowVariableReferences = entityVariable.variableReferences();
            for (var shadowVariableReference : shadowVariableReferences) {
                var isVariableChanged = shadowVariableReference.update(entity, false, changedVariableNotifier);
                if (isVariableChanged) {
                    for (var nextNode : cachedComponentForwardEdges[changedNodeId]) {
                        changeTracker.offer(nextNode);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public List<VariableLoop> getVariableLoops() {
        return Collections.emptyList();
    }
}
