package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.jspecify.annotations.NonNull;

final class DefaultVariableReferenceGraph<Solution_> extends AbstractVariableReferenceGraph<Solution_, BitSet> {
    // These structures are mutable.
    private final Consumer<BitSet> affectedEntitiesUpdater;

    public DefaultVariableReferenceGraph(VariableReferenceGraphBuilder<Solution_> outerGraph,
            IntFunction<TopologicalOrderGraph> graphCreator) {
        super(outerGraph, graphCreator);

        var entityToVariableReferenceMap = new IdentityHashMap<Object, List<EntityVariablePair<Solution_>>>();
        for (var instance : instanceList) {
            var entity = instance.entity();
            entityToVariableReferenceMap.computeIfAbsent(entity, ignored -> new ArrayList<>())
                    .add(instance);
        }
        // Immutable optimized version of the map, now that it won't be updated anymore.
        var immutableEntityToVariableReferenceMap = mapOfListsDeepCopyOf(entityToVariableReferenceMap);
        // This mutable structure is created once, and reused from there on.
        // Otherwise its internal collections were observed being re-created so often
        // that the allocation of arrays would become a major bottleneck.
        affectedEntitiesUpdater = new AffectedEntitiesUpdater<>(graph, instanceList, immutableEntityToVariableReferenceMap::get,
                outerGraph.changedVariableNotifier);
    }

    @Override
    protected BitSet createChangeSet(int instanceCount) {
        return new BitSet(instanceCount);
    }

    @Override
    public void markChanged(@NonNull EntityVariablePair<Solution_> node) {
        changeSet.set(node.graphNodeId());
    }

    @Override
    public void updateChanged() {
        if (changeSet.isEmpty()) {
            return;
        }
        graph.commitChanges(changeSet);
        affectedEntitiesUpdater.accept(changeSet);
    }
}
