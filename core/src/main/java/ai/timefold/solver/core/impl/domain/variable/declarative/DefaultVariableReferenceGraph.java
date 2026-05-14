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

    public DefaultVariableReferenceGraph(VariableReferenceGraphBuilder<Solution_> outerGraph,
            IntFunction<TopologicalOrderGraph> graphCreator) {
        super(outerGraph, graphCreator);

        var entityToVariableReferenceMap = new IdentityHashMap<Object, List<GraphNode<Solution_>>>();
        for (var instance : nodeList) {
            if (instance.groupEntityIds() == null) {
                var entity = instance.entity();
                entityToVariableReferenceMap.computeIfAbsent(entity, ignored -> new ArrayList<>())
                        .add(instance);
            } else {
                for (var groupEntity : instance.variableReferences().get(0).groupEntities()) {
                    entityToVariableReferenceMap.computeIfAbsent(groupEntity, ignored -> new ArrayList<>())
                            .add(instance);
                }
            }
        }
        // Immutable optimized version of the map, now that it won't be updated anymore.
        var immutableEntityToVariableReferenceMap = mapOfListsDeepCopyOf(entityToVariableReferenceMap);
        // This mutable structure is created once, and reused from there on.
        // Otherwise its internal collections were observed being re-created so often
        // that the allocation of arrays would become a major bottleneck.
        affectedEntitiesUpdater =
                new AffectedEntitiesUpdater<>(graph, nodeList, nodeTopologicalOrders,
                        immutableEntityToVariableReferenceMap::get,
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
    void innerUpdateChanged() {
        if (changeTracker.isEmpty()) {
            return;
        }
        graph.commitChanges(changeTracker);
        affectedEntitiesUpdater.accept(changeTracker);
    }

    /**
     * See {@link ConsistencyTracker#setUnknownConsistencyFromEntityShadowVariablesInconsistent}
     */
    public void setUnknownInconsistencyValues() {
        graph.commitChanges(changeTracker);
        affectedEntitiesUpdater.setUnknownInconsistencyValues();
    }
}
