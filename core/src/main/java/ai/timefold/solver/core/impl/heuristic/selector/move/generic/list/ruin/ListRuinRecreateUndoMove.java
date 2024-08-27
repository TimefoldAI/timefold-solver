package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NavigableSet;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractUndoMove;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

public final class ListRuinRecreateUndoMove<Solution_> extends AbstractUndoMove<Solution_> {

    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final Map<Object, NavigableSet<RuinedLocation>> entityToOriginalPositionMap;
    private final Map<Object, NavigableSet<RuinedLocation>> entityToNewPositionMap;

    ListRuinRecreateUndoMove(ListRuinRecreateMove<Solution_> parentMove,
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            Map<Object, NavigableSet<RuinedLocation>> entityToOriginalPositionMap,
            Map<Object, NavigableSet<RuinedLocation>> entityToNewPositionMap) {
        super(parentMove);
        this.listVariableDescriptor = listVariableDescriptor;
        this.entityToOriginalPositionMap = entityToOriginalPositionMap;
        this.entityToNewPositionMap = entityToNewPositionMap;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var innerScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        var changedEntities = Collections.newSetFromMap(new IdentityHashMap<>());
        changedEntities.addAll(entityToOriginalPositionMap.keySet());
        changedEntities.addAll(entityToNewPositionMap.keySet());

        for (var entity : changedEntities) {
            innerScoreDirector.beforeListVariableChanged(listVariableDescriptor, entity,
                    listVariableDescriptor.getFirstUnpinnedIndex(entity),
                    listVariableDescriptor.getListSize(entity));
        }

        for (var entry : entityToOriginalPositionMap.entrySet()) {
            var entity = entry.getKey();
            for (var position : entry.getValue().descendingSet()) {
                listVariableDescriptor.removeElement(entity, position.index());
            }
        }

        for (var entry : entityToNewPositionMap.entrySet()) {
            var entity = entry.getKey();
            for (var position : entry.getValue()) {
                listVariableDescriptor.addElement(entity, position.index(), position.ruinedValue());
            }
        }

        for (var entity : changedEntities) {
            innerScoreDirector.afterListVariableChanged(listVariableDescriptor, entity,
                    listVariableDescriptor.getFirstUnpinnedIndex(entity),
                    listVariableDescriptor.getListSize(entity));
        }

        scoreDirector.triggerVariableListeners();
    }

}
