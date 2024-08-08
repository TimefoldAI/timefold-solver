package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Arrays;
import java.util.Collection;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

public final class RuinUndoMove<Solution_> extends AbstractMove<Solution_> {

    private final Object[] ruinedEntities;
    private final GenuineVariableDescriptor<Solution_> genuineVariableDescriptor;
    private final Object[] recordedNewValues;

    RuinUndoMove(Object[] ruinedEntities, GenuineVariableDescriptor<Solution_> genuineVariableDescriptor,
            Object[] recordedNewValues) {
        this.ruinedEntities = ruinedEntities;
        this.genuineVariableDescriptor = genuineVariableDescriptor;
        this.recordedNewValues = recordedNewValues;
    }

    @Override
    protected Move<Solution_> createUndoMove(ScoreDirector<Solution_> scoreDirector) {
        throw new UnsupportedOperationException("Impossible state: the undo move (%s) cannot be undone."
                .formatted(this));
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var innerScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        for (int i = 0; i < ruinedEntities.length; i++) {
            innerScoreDirector.beforeVariableChanged(genuineVariableDescriptor, ruinedEntities[i]);
            genuineVariableDescriptor.setValue(ruinedEntities[i], recordedNewValues[i]);
            innerScoreDirector.afterVariableChanged(genuineVariableDescriptor, ruinedEntities[i]);
        }
    }

    @Override
    public Collection<?> getPlanningEntities() {
        return Arrays.asList(ruinedEntities);
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return true;
    }

    @Override
    public String toString() {
        return "RuinUndoMove{" +
                "entities=" + Arrays.toString(ruinedEntities) +
                ", newValues=" + ((recordedNewValues != null) ? Arrays.toString(recordedNewValues) : "?") +
                '}';
    }
}
