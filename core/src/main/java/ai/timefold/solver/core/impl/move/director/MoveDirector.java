package ai.timefold.solver.core.impl.move.director;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.move.InnerMutableSolutionView;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementLocation;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public sealed class MoveDirector<Solution_>
        implements InnerMutableSolutionView<Solution_>, Rebaser
        permits EphemeralMoveDirector {

    protected final VariableDescriptorAwareScoreDirector<Solution_> scoreDirector;

    public MoveDirector(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        this.scoreDirector = Objects.requireNonNull(scoreDirector);
    }

    @Override
    public <Entity_, Value_> void assignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ planningValue, Entity_ destinationEntity, int destinationIndex) {
        var variableDescriptor =
                ((DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
        scoreDirector.beforeListVariableElementAssigned(variableDescriptor, planningValue);
        scoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex, destinationIndex);
        variableDescriptor.addElement(destinationEntity, destinationIndex, planningValue);
        scoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex, destinationIndex + 1);
        scoreDirector.afterListVariableElementAssigned(variableDescriptor, planningValue);
    }

    @Override
    public <Entity_, Value_> void unassignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ movedValue, Entity_ sourceEntity, int sourceIndex) {
        var variableDescriptor =
                ((DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
        scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, movedValue);
        scoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex + 1);
        ((List<Value_>) variableDescriptor.getValue(sourceEntity))
                .remove(sourceIndex);
        scoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex);
        scoreDirector.afterListVariableElementUnassigned(variableDescriptor, movedValue);
    }

    public final <Entity_, Value_> void changeVariable(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, @Nullable Value_ newValue) {
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        scoreDirector.beforeVariableChanged(variableDescriptor, entity);
        variableDescriptor.setValue(entity, newValue);
        scoreDirector.afterVariableChanged(variableDescriptor, entity);
    }

    @SuppressWarnings("unchecked")
    public final <Entity_, Value_> @Nullable Value_ moveValueBetweenLists(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ sourceEntity, int sourceIndex,
            Entity_ destinationEntity, int destinationIndex) {
        if (sourceEntity == destinationEntity) {
            return moveValueInList(variableMetaModel, sourceEntity, sourceIndex, destinationIndex);
        }
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        scoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex + 1);
        var element = (Value_) variableDescriptor.removeElement(sourceEntity, sourceIndex);
        scoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex);

        scoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex, destinationIndex);
        variableDescriptor.addElement(destinationEntity, destinationIndex, element);
        scoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex, destinationIndex + 1);

        return element;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <Entity_, Value_> @Nullable Value_ moveValueInList(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity, int sourceIndex,
            int destinationIndex) {
        if (sourceIndex == destinationIndex) {
            return null;
        } else if (sourceIndex > destinationIndex) { // Always start from the lower index.
            return moveValueInList(variableMetaModel, entity, destinationIndex, sourceIndex);
        }
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        var toIndex = destinationIndex + 1;
        scoreDirector.beforeListVariableChanged(variableDescriptor, entity, sourceIndex, toIndex);
        var variable = (List<Value_>) variableDescriptor.getValue(entity);
        var value = variable.remove(sourceIndex);
        variable.add(destinationIndex, value);
        scoreDirector.afterListVariableChanged(variableDescriptor, entity, sourceIndex, toIndex);
        return value;
    }

    @Override
    public final void updateShadowVariables() {
        updateShadowVariables(false); // Called by the move itself.
    }

    public final void updateShadowVariables(boolean comingFromScoreDirector) {
        if (!comingFromScoreDirector) { // Prevent recursion.
            scoreDirector.triggerVariableListeners();
        }
    }

    @Override
    public final <Entity_, Value_> Value_ getValue(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity) {
        return extractVariableDescriptor(variableMetaModel).getValue(entity);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <Entity_, Value_> Value_ getValueAtIndex(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity, int index) {
        return (Value_) extractVariableDescriptor(variableMetaModel).getValue(entity).get(index);
    }

    @Override
    public <Entity_, Value_> ElementLocation
            getPositionOf(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value) {
        return getPositionOf((InnerScoreDirector<Solution_, ?>) scoreDirector, variableMetaModel, value);
    }

    protected static <Solution_, Entity_, Value_> ElementLocation getPositionOf(InnerScoreDirector<Solution_, ?> scoreDirector,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value) {
        return scoreDirector.getListVariableStateSupply(extractVariableDescriptor(variableMetaModel))
                .getLocationInList(value);
    }

    @Override
    public final <T> @Nullable T rebase(@Nullable T problemFactOrPlanningEntity) {
        return scoreDirector.lookUpWorkingObject(problemFactOrPlanningEntity);
    }

    private static <Solution_, Entity_, Value_> BasicVariableDescriptor<Solution_>
            extractVariableDescriptor(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return ((DefaultPlanningVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
    }

    private static <Solution_, Entity_, Value_> ListVariableDescriptor<Solution_>
            extractVariableDescriptor(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return ((DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
    }

    /**
     * Moves that are to be undone later need to be run with the instance returned by this method.
     * 
     * @return never null
     */
    public EphemeralMoveDirector<Solution_> ephemeral() {
        return new EphemeralMoveDirector<>(scoreDirector);
    }

    @Override
    public VariableDescriptorAwareScoreDirector<Solution_> getScoreDirector() {
        return scoreDirector;
    }

    public void resetWorkingSolution(Solution_ workingSolution) {

    }

}
