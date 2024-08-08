package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NavigableSet;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

public final class ListRuinUndoMove<Solution_> extends AbstractMove<Solution_> {

    private final Object[] ruinedValues;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final Map<Object, NavigableSet<RuinedLocation>> entityToOriginalPositionMap;
    private final Map<Object, NavigableSet<RuinedLocation>> entityToNewPositionMap;

    ListRuinUndoMove(Object[] ruinedValues, ListVariableDescriptor<Solution_> listVariableDescriptor,
            Map<Object, NavigableSet<RuinedLocation>> entityToOriginalPositionMap,
            Map<Object, NavigableSet<RuinedLocation>> entityToNewPositionMap) {
        this.ruinedValues = ruinedValues;
        this.listVariableDescriptor = listVariableDescriptor;
        this.entityToOriginalPositionMap = entityToOriginalPositionMap;
        this.entityToNewPositionMap = entityToNewPositionMap;
    }

    @Override
    protected Move<Solution_> createUndoMove(ScoreDirector<Solution_> scoreDirector) {
        throw new UnsupportedOperationException("Impossible state: the undo move (%s) cannot be undone."
                .formatted(this));
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
                listVariableDescriptor.removeElement(entity,
                        position.index());
            }
        }

        for (var entry : entityToNewPositionMap.entrySet()) {
            var entity = entry.getKey();
            for (var position : entry.getValue()) {
                listVariableDescriptor.addElement(entity,
                        position.index(),
                        position.ruinedValue());
            }
        }

        for (var entity : changedEntities) {
            innerScoreDirector.afterListVariableChanged(listVariableDescriptor, entity,
                    listVariableDescriptor.getFirstUnpinnedIndex(entity),
                    listVariableDescriptor.getListSize(entity));
        }

        scoreDirector.triggerVariableListeners();
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return true;
    }

    @Override
    public String toString() {
        return "ListRuinUndoMove{" +
                "values=" + Arrays.toString(ruinedValues) +
                ", newLocationsByEntity=" + (!entityToNewPositionMap.isEmpty() ? entityToNewPositionMap : "?") +
                '}';
    }
}
