package ai.timefold.solver.core.impl.move.director;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.metamodel.BasicVariableMetaModel;
import ai.timefold.solver.core.api.domain.metamodel.ElementLocation;
import ai.timefold.solver.core.api.domain.metamodel.ListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultBasicVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.move.InnerMutableSolutionState;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

/**
 * Runs moves. For moves that are to be undone later, see {@link #undoable()}.
 * 
 * @param <Solution_>
 */
public class MoveDirector<Solution_> implements InnerMutableSolutionState<Solution_> {

    protected final VariableDescriptorAwareScoreDirector<Solution_> scoreDirector;

    public MoveDirector(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        this.scoreDirector = Objects.requireNonNull(scoreDirector);
    }

    public <Entity_, Value_> void changeVariable(BasicVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, Value_ newValue) {
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        scoreDirector.beforeVariableChanged(variableDescriptor, entity);
        variableDescriptor.setValue(entity, newValue);
        scoreDirector.afterVariableChanged(variableDescriptor, entity);
    }

    public <Entity_, Value_> void moveValueBetweenLists(ListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ sourceEntity, int sourceIndex, Entity_ destinationEntity, int destinationIndex) {
        if (sourceEntity == destinationEntity) {
            moveValueInList(variableMetaModel, sourceEntity, sourceIndex, destinationIndex);
            return;
        }
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        scoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex + 1);
        var element = variableDescriptor.removeElement(sourceEntity, sourceIndex);
        scoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex);

        scoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex);
        variableDescriptor.addElement(destinationEntity, destinationIndex, element);
        scoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex, destinationIndex + 1);

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
        scoreDirector.beforeListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
        var variable = variableDescriptor.getValue(entity);
        var value = variable.remove(sourceIndex);
        variable.add(destinationIndex, value);
        scoreDirector.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
    }

    @Override
    public void updateShadowVariables() {
        scoreDirector.triggerVariableListeners();
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
        return getPositionOf((AbstractScoreDirector<Solution_, ?, ?>) scoreDirector, variableMetaModel, value);
    }

    protected <Entity_, Value_> ElementLocation getPositionOf(AbstractScoreDirector<Solution_, ?, ?> scoreDirector,
            ListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value) {
        return scoreDirector.getListVariableStateSupply(extractVariableDescriptor(variableMetaModel))
                .getLocationInList(value);
    }

    @Override
    public <T> T rebase(T problemFactOrPlanningEntity) {
        return scoreDirector.lookUpWorkingObject(problemFactOrPlanningEntity);
    }

    private static <Solution_, Entity_, Value_> BasicVariableDescriptor<Solution_>
            extractVariableDescriptor(BasicVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return ((DefaultBasicVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
    }

    private static <Solution_, Entity_, Value_> ListVariableDescriptor<Solution_>
            extractVariableDescriptor(ListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return ((DefaultListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
    }

    /**
     * Moves that are to be undone later need to be run with the instance returned by this method.
     * 
     * @return never null
     */
    public UndoableMoveDirector<Solution_> undoable() {
        return new UndoableMoveDirector<>(scoreDirector);
    }

    @Override
    public VariableDescriptorAwareScoreDirector<Solution_> getScoreDirector() {
        return scoreDirector;
    }
}
