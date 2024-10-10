package ai.timefold.solver.core.impl.move.director;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.metamodel.BasicVariableMetaModel;
import ai.timefold.solver.core.api.domain.metamodel.ElementLocation;
import ai.timefold.solver.core.api.domain.metamodel.ListVariableMetaModel;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultBasicVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.move.InnerMutableSolutionState;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

public final class MoveDirector<Solution_> implements InnerMutableSolutionState<Solution_> {

    private final AbstractScoreDirector<Solution_, ?, ?> scoreDirector;
    private final VariableChangeRecordingScoreDirector<Solution_> changeRecordingScoreDirector;

    public MoveDirector(InnerScoreDirector<Solution_, ?> scoreDirector) {
        this.scoreDirector = (AbstractScoreDirector<Solution_, ?, ?>) Objects.requireNonNull(scoreDirector);
        // Doesn't require the index cache, because we maintain the invariant in this class.
        this.changeRecordingScoreDirector = new VariableChangeRecordingScoreDirector<>(scoreDirector, false);
    }

    @Override
    public <Entity_, Value_> void changeVariable(BasicVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, Value_ newValue) {
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        changeRecordingScoreDirector.beforeVariableChanged(variableDescriptor, entity);
        variableDescriptor.setValue(entity, newValue);
        changeRecordingScoreDirector.afterVariableChanged(variableDescriptor, entity);
    }

    @Override
    public <Entity_, Value_> void moveValueBetweenLists(ListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ sourceEntity, int sourceIndex, Entity_ destinationEntity, int destinationIndex) {
        if (sourceEntity == destinationEntity) {
            moveValueInList(variableMetaModel, sourceEntity, sourceIndex, destinationIndex);
            return;
        }
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        changeRecordingScoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex + 1);
        var element = variableDescriptor.removeElement(sourceEntity, sourceIndex);
        changeRecordingScoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex);

        changeRecordingScoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex);
        variableDescriptor.addElement(destinationEntity, destinationIndex, element);
        changeRecordingScoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex + 1);

    }

    @Override
    public <Entity_, Value_> void moveValueInList(ListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, int sourceIndex, int destinationIndex) {
        if (sourceIndex == destinationIndex) {
            return;
        } else if (sourceIndex > destinationIndex) { // Always start from the lower index.
            moveValueInList(variableMetaModel, entity, destinationIndex, sourceIndex);
            return;
        }
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        var fromIndex = Math.min(sourceIndex, destinationIndex);
        var toIndex = Math.max(sourceIndex, destinationIndex) + 1;
        changeRecordingScoreDirector.beforeListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
        var variable = variableDescriptor.getValue(entity);
        var value = variable.remove(sourceIndex);
        variable.add(destinationIndex, value);
        changeRecordingScoreDirector.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
    }

    @Override
    public void updateShadowVariables() {
        changeRecordingScoreDirector.triggerVariableListeners();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Entity_, Value_> Value_ getValue(BasicVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity) {
        return (Value_) extractVariableDescriptor(variableMetaModel).getValue(entity);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Entity_, Value_> Value_ getValueAtIndex(ListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, int index) {
        return (Value_) extractVariableDescriptor(variableMetaModel).getValue(entity).get(index);
    }

    @Override
    public <Entity_, Value_> ElementLocation getPositionOf(ListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value) {
        return scoreDirector.getListVariableStateSupply(extractVariableDescriptor(variableMetaModel))
                .getLocationInList(value);
    }

    @Override
    public <T> T rebase(T problemFactOrPlanningEntity) {
        return scoreDirector.lookUpWorkingObject(problemFactOrPlanningEntity);
    }

    public void undo() {
        changeRecordingScoreDirector.undoChanges();
    }

    private static <Solution_, Entity_, Value_> BasicVariableDescriptor<Solution_>
            extractVariableDescriptor(BasicVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return ((DefaultBasicVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
    }

    private static <Solution_, Entity_, Value_> ListVariableDescriptor<Solution_>
            extractVariableDescriptor(ListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return ((DefaultListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
    }

    @Override
    public ScoreDirector<Solution_> getVariableChangeRecordingScoreDirector() {
        return changeRecordingScoreDirector;
    }

}
