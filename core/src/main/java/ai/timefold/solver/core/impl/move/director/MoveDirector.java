package ai.timefold.solver.core.impl.move.director;

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

import org.jspecify.annotations.NonNull;

public sealed class MoveDirector<Solution_>
        implements InnerMutableSolutionView<Solution_>, Rebaser
        permits EphemeralMoveDirector {

    protected final VariableDescriptorAwareScoreDirector<Solution_> scoreDirector;
    private final MoveStreamSession<Solution_> moveStreamSession;

    public MoveDirector(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        this(scoreDirector, null);
    }

    public MoveDirector(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector,
            MoveStreamSession<Solution_> moveStreamSession) {
        this.scoreDirector = Objects.requireNonNull(scoreDirector);
        this.moveStreamSession = moveStreamSession;
    }

    @Override
    public <Entity_, Value_> void assignValue(
            @NonNull PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, @NonNull Value_ planningValue,
            @NonNull Entity_ destinationEntity, int destinationIndex) {

    }

    @Override
    public <Entity_, Value_> void unassignValue(
            @NonNull PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, @NonNull Value_ movedValue,
            @NonNull Entity_ sourceEntity, int sourceIndex) {

    }

    public final <Entity_, Value_> void changeVariable(
            @NonNull PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @NonNull Entity_ entity, Value_ newValue) {
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        scoreDirector.beforeVariableChanged(variableDescriptor, entity);
        variableDescriptor.setValue(entity, newValue);
        scoreDirector.afterVariableChanged(variableDescriptor, entity);
        if (moveStreamSession != null) {
            moveStreamSession.update(entity);
        }
    }

    @SuppressWarnings("unchecked")
    public final <Entity_, Value_> Value_ moveValueBetweenLists(
            @NonNull PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @NonNull Entity_ sourceEntity, int sourceIndex, @NonNull Entity_ destinationEntity, int destinationIndex) {
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

        if (moveStreamSession != null) {
            moveStreamSession.update(sourceEntity);
            moveStreamSession.update(destinationEntity);
        }
        return element;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <Entity_, Value_> Value_ moveValueInList(
            @NonNull PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @NonNull Entity_ entity, int sourceIndex, int destinationIndex) {
        if (sourceIndex == destinationIndex) {
            return null;
        } else if (sourceIndex > destinationIndex) { // Always start from the lower index.
            return moveValueInList(variableMetaModel, entity, destinationIndex, sourceIndex);
        }
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        var toIndex = destinationIndex + 1;
        scoreDirector.beforeListVariableChanged(variableDescriptor, entity, sourceIndex, toIndex);
        var variable = variableDescriptor.getValue(entity);
        var value = (Value_) variable.remove(sourceIndex);
        variable.add(destinationIndex, value);
        scoreDirector.afterListVariableChanged(variableDescriptor, entity, sourceIndex, toIndex);
        if (moveStreamSession != null) {
            moveStreamSession.update(entity);
        }
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
        if (moveStreamSession != null) {
            moveStreamSession.settle();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <Entity_, Value_> Value_ getValue(
            @NonNull PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @NonNull Entity_ entity) {
        return (Value_) extractVariableDescriptor(variableMetaModel).getValue(entity);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <Entity_, Value_> @NonNull Value_ getValueAtIndex(
            @NonNull PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @NonNull Entity_ entity, int index) {
        return (Value_) extractVariableDescriptor(variableMetaModel).getValue(entity).get(index);
    }

    @Override
    public <Entity_, Value_> @NonNull ElementLocation getPositionOf(
            @NonNull PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @NonNull Value_ value) {
        return getPositionOf((InnerScoreDirector<Solution_, ?>) scoreDirector, variableMetaModel, value);
    }

    protected static <Solution_, Entity_, Value_> ElementLocation getPositionOf(InnerScoreDirector<Solution_, ?> scoreDirector,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value) {
        return scoreDirector.getListVariableStateSupply(extractVariableDescriptor(variableMetaModel))
                .getLocationInList(value);
    }

    @Override
    public final <T> T rebase(@NonNull T problemFactOrPlanningEntity) {
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
        if (moveStreamSession == null) {
            return;
        }
        moveStreamSession.resetWorkingSolution(workingSolution);
        scoreDirector.getSolutionDescriptor().visitAll(workingSolution, moveStreamSession::insert);
    }

}
