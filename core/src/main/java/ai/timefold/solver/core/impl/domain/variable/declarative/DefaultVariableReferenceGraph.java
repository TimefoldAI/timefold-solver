package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.IntFunction;

import org.jspecify.annotations.NonNull;

final class DefaultVariableReferenceGraph<Solution_> extends AbstractVariableReferenceGraph<Solution_, BitSet> {
    // These structures are mutable.
    private final AffectedEntitiesUpdater<Solution_> affectedEntitiesUpdater;
    private final boolean ignoreInconsistentSolutions;

    public DefaultVariableReferenceGraph(VariableReferenceGraphBuilder<Solution_> outerGraph,
            IntFunction<TopologicalOrderGraph> graphCreator,
            boolean ignoreInconsistentSolutions) {
        super(outerGraph, graphCreator);
        this.ignoreInconsistentSolutions = ignoreInconsistentSolutions;
        var entityToVariableReferenceMap = new IdentityHashMap<Object, List<GraphNode<Solution_>>>();
        for (var instance : nodeList) {
            if (instance.groupEntityIds() == null) {
                var entity = instance.entity();
                entityToVariableReferenceMap.computeIfAbsent(entity, ignored -> new ArrayList<>())
                        .add(instance);
            } else {
                for (var groupEntity : instance.variableReferences().getFirst().groupEntities()) {
                    entityToVariableReferenceMap.computeIfAbsent(groupEntity, ignored -> new ArrayList<>())
                            .add(instance);
                }
            }
        }
        // This mutable structure is created once, and reused from there on.
        // Otherwise its internal collections were observed being re-created so often
        // that the allocation of arrays would become a major bottleneck.
        affectedEntitiesUpdater =
                new AffectedEntitiesUpdater<>(graph, nodeList, nodeTopologicalOrders,
                        entityToVariableReferenceMap::get,
                        outerGraph.entityToEntityId.size(), outerGraph.changedVariableNotifier);
    }

    @Override
    protected BitSet createChangeTracker(int instanceCount) {
        return new BitSet(instanceCount);
    }

    @Override
    void markChanged(@NonNull GraphNode<Solution_> node) {
        changeTracker.set(node.graphNodeId());
    }

    @Override
    boolean innerUpdateChanged() {
        if (changeTracker.isEmpty()) {
            return true;
        }
        if (graph.commitChanges(changeTracker) && ignoreInconsistentSolutions) {
            return false;
        } else {
            affectedEntitiesUpdater.accept(changeTracker);
            return true;
        }
    }

    /**
     * See {@link ConsistencyTracker#setUnknownConsistencyFromEntityShadowVariablesInconsistent}
     */
    public void setUnknownInconsistencyValues() {
        graph.commitChanges(changeTracker);
        affectedEntitiesUpdater.setUnknownInconsistencyValues();
    }
}
