package ai.timefold.solver.core.impl.move;

import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.InnerGenuineVariableMetaModel;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.MoveAdapters;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.UnassignedElement;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public sealed class MoveDirector<Solution_, Score_ extends Score<Score_>>
        implements InnerMutableSolutionView<Solution_>, Rebaser permits EphemeralMoveDirector {

    protected final VariableDescriptorAwareScoreDirector<Solution_> externalScoreDirector;
    private final InnerScoreDirector<Solution_, Score_> backingScoreDirector;

    public MoveDirector(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        this.backingScoreDirector = Objects.requireNonNull(scoreDirector);
        if (EphemeralMoveDirector.class.isAssignableFrom(getClass())) {
            // Ephemeral move director records operations for a later undo,
            // and the external director is no longer an instance of InnerScoreDirector.
            // However, some pieces of code need methods from InnerScoreDirector,
            // in which case we turn to the backing score director.
            // This is only safe for operations that do not need to be undone, such as calculateScore().
            // Operations which need undo must go through the external score director, which is recording in this case.
            this.externalScoreDirector = new VariableChangeRecordingScoreDirector<>(scoreDirector, false);
        } else {
            this.externalScoreDirector = scoreDirector;
        }
    }

    @Override
    public final <Entity_, Value_> void assignValueAndAdd(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ planningValue,
            Entity_ destinationEntity, int destinationIndex) {
        if (!(getPositionOf(variableMetaModel, planningValue) instanceof UnassignedElement)) {
            throw new IllegalStateException("Cannot assign an already assigned value (%s).".formatted(planningValue));
        }
        var variableDescriptor =
                ((DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
        externalScoreDirector.beforeListVariableElementAssigned(variableDescriptor, planningValue);
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex);
        variableDescriptor.addElement(destinationEntity, destinationIndex, planningValue);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex + 1);
        externalScoreDirector.afterListVariableElementAssigned(variableDescriptor, planningValue);
        externalScoreDirector.triggerVariableListeners();
    }

    @Override
    public final <Entity_, Value_> void assignValueAndSet(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ planningValue,
            Entity_ destinationEntity, int destinationIndex) {
        if (destinationIndex == countValues(variableMetaModel, destinationEntity)) {
            // Faster code path, no need to unassign anything.
            assignValueAndAdd(variableMetaModel, planningValue, destinationEntity, destinationIndex);
            return;
        }

        if (!(getPositionOf(variableMetaModel, planningValue) instanceof UnassignedElement)) {
            throw new IllegalStateException("Cannot assign an already assigned value (%s).".formatted(planningValue));
        }

        var oldValue = getValueAtIndex(variableMetaModel, destinationEntity, destinationIndex);
        var variableDescriptor =
                ((DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
        externalScoreDirector.beforeListVariableElementAssigned(variableDescriptor, planningValue);
        externalScoreDirector.beforeListVariableElementUnassigned(variableDescriptor, oldValue);
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex + 1);
        var actualOldValue = variableDescriptor.setElement(destinationEntity, destinationIndex, planningValue);
        if (oldValue != actualOldValue) {
            throw new IllegalStateException(
                    "Impossible state: The value (%s) at index (%d) of entity (%s) is not as expected (%s)."
                            .formatted(actualOldValue, destinationIndex, destinationEntity, oldValue));
        }
        externalScoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex + 1);
        externalScoreDirector.afterListVariableElementUnassigned(variableDescriptor, oldValue);
        externalScoreDirector.afterListVariableElementAssigned(variableDescriptor, planningValue);

        externalScoreDirector.triggerVariableListeners();
    }

    @Override
    public <Entity_, Value_> void unassignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value) {
        var locationInList = getPositionOf(variableMetaModel, value).ensureAssigned(() -> """
                The value (%s) is not assigned to a list variable.
                This may indicate score corruption or a problem with the move's implementation.""".formatted(value));
        unassignValue(variableMetaModel, value, locationInList.entity(), locationInList.index());
    }

    @Override
    public <Entity_, Value_> Value_ unassignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, int index) {
        var value = getValueAtIndex(variableMetaModel, entity, index);
        unassignValue(variableMetaModel, value, entity, index);
        return value;
    }

    private <Entity_, Value_> void unassignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ movedValue, Entity_ entity, int index) {
        var variableDescriptor =
                ((DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
        externalScoreDirector.beforeListVariableElementUnassigned(variableDescriptor, movedValue);
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, entity, index, index + 1);
        variableDescriptor.getValue(entity).remove(index);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, entity, index, index);
        externalScoreDirector.afterListVariableElementUnassigned(variableDescriptor, movedValue);
        externalScoreDirector.triggerVariableListeners();
    }

    @Override
    public final <Entity_, Value_> void changeVariable(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, @Nullable Value_ newValue) {
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        externalScoreDirector.beforeVariableChanged(variableDescriptor, entity);
        variableDescriptor.setValue(entity, newValue);
        externalScoreDirector.afterVariableChanged(variableDescriptor, entity);
        externalScoreDirector.triggerVariableListeners();
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <Entity_, Value_> Value_ moveValueBetweenLists(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ sourceEntity, int sourceIndex,
            Entity_ destinationEntity, int destinationIndex) {
        if (sourceEntity == destinationEntity) {
            // Moving within the same list is not supported by this method.
            // This avoids confusion about the shifting of indices when removing and adding within the same list.
            throw new IllegalArgumentException(
                    "Source entity (%s) and destination entity (%s) must be different when moving values between lists."
                            .formatted(sourceEntity, destinationEntity));
        }
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex + 1);
        var element = (Value_) variableDescriptor.removeElement(sourceEntity, sourceIndex);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex);

        externalScoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex);
        variableDescriptor.addElement(destinationEntity, destinationIndex, element);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex + 1);

        externalScoreDirector.triggerVariableListeners();
        return element;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <Entity_, Value_> Value_ moveValueInList(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ sourceEntity, int sourceIndex,
            int destinationIndex) {
        if (sourceIndex == destinationIndex) {
            throw new IllegalArgumentException(
                    "When moving values in the same list, sourceIndex (%d) and destinationIndex (%d) must be different."
                            .formatted(sourceIndex, destinationIndex));
        } else if (sourceIndex < 0 || destinationIndex < 0) {
            throw new IllegalArgumentException(
                    "The sourceIndex (%d) and destinationIndex (%d) must both be >= 0."
                            .formatted(sourceIndex, destinationIndex));
        }

        var fromIndex = Math.min(sourceIndex, destinationIndex);
        var toIndex = Math.max(sourceIndex, destinationIndex) + 1;

        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        var list = variableDescriptor.getValue(sourceEntity);
        var listSize = list.size();
        if (sourceIndex >= listSize) {
            throw new IllegalArgumentException(
                    "The sourceIndex (%d) must be less than the list size (%d).".formatted(sourceIndex, listSize));
        } else if (destinationIndex > listSize) { // destinationIndex == listSize is allowed (append to the end of the list)
            throw new IllegalArgumentException(
                    "The destinationIndex (%d) must be less than or equal to the list size (%d)."
                            .formatted(destinationIndex, listSize));
        }

        externalScoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, fromIndex, toIndex);
        var element = (Value_) list.remove(sourceIndex);
        list.add(destinationIndex, element);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, fromIndex, toIndex);
        externalScoreDirector.triggerVariableListeners();
        return element;
    }

    @Override
    public <Entity_, Value_> Value_ shiftValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ sourceEntity, int sourceIndex, int offset) {
        if (offset == 0) {
            throw new IllegalArgumentException("The offset (%d) must not be zero.".formatted(offset));
        }
        var destinationIndex = sourceIndex + offset;
        return moveValueInList(variableMetaModel, sourceEntity, sourceIndex, destinationIndex);
    }

    @Override
    public <Entity_, Value_> void swapValuesBetweenLists(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ leftEntity, int leftIndex,
            Entity_ rightEntity, int rightIndex) {
        if (leftEntity == rightEntity) {
            swapValuesInList(variableMetaModel, leftEntity, leftIndex, rightIndex);
        } else {
            var variableDescriptor = extractVariableDescriptor(variableMetaModel);
            var leftElement = variableDescriptor.getElement(leftEntity, leftIndex);
            var rightElement = variableDescriptor.getElement(rightEntity, rightIndex);

            externalScoreDirector.beforeListVariableChanged(variableDescriptor, leftEntity, leftIndex, leftIndex + 1);
            externalScoreDirector.beforeListVariableChanged(variableDescriptor, rightEntity, rightIndex, rightIndex + 1);
            variableDescriptor.setElement(leftEntity, leftIndex, rightElement);
            variableDescriptor.setElement(rightEntity, rightIndex, leftElement);
            externalScoreDirector.afterListVariableChanged(variableDescriptor, leftEntity, leftIndex, leftIndex + 1);
            externalScoreDirector.afterListVariableChanged(variableDescriptor, rightEntity, rightIndex, rightIndex + 1);
            externalScoreDirector.triggerVariableListeners();
        }
    }

    @Override
    public <Entity_, Value_> void swapValuesInList(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, int leftIndex, int rightIndex) {
        if (leftIndex == rightIndex) {
            throw new IllegalArgumentException(
                    "When swapping values in the same list, leftIndex (%d) and rightIndex (%d) must be different."
                            .formatted(leftIndex, rightIndex));
        }

        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        var leftElement = variableDescriptor.getElement(entity, leftIndex);
        var rightElement = variableDescriptor.getElement(entity, rightIndex);

        var fromIndex = Math.min(leftIndex, rightIndex);
        var toIndex = Math.max(leftIndex, rightIndex) + 1;
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
        var list = variableDescriptor.getValue(entity);
        list.set(leftIndex, rightElement);
        list.set(rightIndex, leftElement);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
        externalScoreDirector.triggerVariableListeners();
    }

    @Override
    public <Entity_, Value_> boolean isValueInRange(GenuineVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @Nullable Entity_ entity, @Nullable Value_ value) {
        var innerGenuineVariableMetaModel = (InnerGenuineVariableMetaModel<Solution_>) variableMetaModel;
        var valueRangeDescriptor = innerGenuineVariableMetaModel.variableDescriptor().getValueRangeDescriptor();
        if (valueRangeDescriptor.canExtractValueRangeFromSolution()) {
            return backingScoreDirector.getValueRangeManager().getFromSolution(valueRangeDescriptor).contains(value);
        } else {
            return backingScoreDirector.getValueRangeManager()
                    .getFromEntity(valueRangeDescriptor, Objects.requireNonNull(entity)).contains(value);
        }
    }

    /**
     * Execute a given move and make sure shadow variables are up to date after that.
     */
    public final void execute(Move<Solution_> move) {
        MoveAdapters.unadapt(move).execute(this);
        externalScoreDirector.triggerVariableListeners();
    }

    public final InnerScore<Score_> executeTemporary(Move<Solution_> move) {
        var ephemeralMoveDirector = ephemeral();
        ephemeralMoveDirector.execute(move);
        var score = backingScoreDirector.calculateScore();
        ephemeralMoveDirector.close(); // This undoes the move.
        return score;
    }

    public <Result_> Result_ executeTemporary(Move<Solution_> move,
            TemporaryMovePostprocessor<Solution_, Score_, Result_> postprocessor) {
        var ephemeralMoveDirector = ephemeral();
        ephemeralMoveDirector.execute(move);
        var score = backingScoreDirector.calculateScore();
        var result = postprocessor.apply(score, ephemeralMoveDirector.createUndoMove());
        ephemeralMoveDirector.close(); // This undoes the move.
        return result;
    }

    @Override
    public final <Entity_, Value_> Value_ getValue(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity) {
        return extractVariableDescriptor(variableMetaModel).getValue(entity);
    }

    @Override
    public <Entity_, Value_> int countValues(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity) {
        return extractVariableDescriptor(variableMetaModel).getValue(entity).size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <Entity_, Value_> Value_ getValueAtIndex(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity, int index) {
        return (Value_) extractVariableDescriptor(variableMetaModel).getValue(entity).get(index);
    }

    @Override
    public <Entity_, Value_> ElementPosition
            getPositionOf(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value) {
        return getPositionOf(backingScoreDirector, variableMetaModel, value);
    }

    @Override
    public <Entity_, Value_> boolean isPinned(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @Nullable Entity_ entity) {
        return isPinned(extractVariableDescriptor(variableMetaModel).getEntityDescriptor(), entity);
    }

    public <Value_> boolean isPinned(EntityDescriptor<Solution_> entityDescriptor, @Nullable Value_ entity) {
        if (entity == null) {
            return false; // Null is never pinned.
        }
        return !entityDescriptor.isMovable(backingScoreDirector.getWorkingSolution(), entity);
    }

    protected static <Solution_, Entity_, Value_> ElementPosition getPositionOf(InnerScoreDirector<Solution_, ?> scoreDirector,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> listVariableDescriptor, Value_ value) {
        return scoreDirector.getListVariableStateSupply(extractVariableDescriptor(listVariableDescriptor))
                .getElementPosition(value);
    }

    @Override
    public <Entity_, Value_> boolean isPinned(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @Nullable Value_ value) {
        return isPinned(extractVariableDescriptor(variableMetaModel), value);
    }

    public <Value_> boolean isPinned(ListVariableDescriptor<Solution_> listVariableDescriptor, @Nullable Value_ value) {
        if (value == null) {
            return false; // Null is never pinned.
        }
        return backingScoreDirector.getListVariableStateSupply(listVariableDescriptor).isPinned(value);
    }

    @Override
    public final <T> @Nullable T rebase(@Nullable T problemFactOrPlanningEntity) {
        return externalScoreDirector.lookUpWorkingObject(problemFactOrPlanningEntity);
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
     * To undo the move, remember to call {@link EphemeralMoveDirector#close()}.
     *
     * @return never null
     */
    final EphemeralMoveDirector<Solution_, Score_> ephemeral() {
        return new EphemeralMoveDirector<>(backingScoreDirector);
    }

    @Override
    public final VariableDescriptorAwareScoreDirector<Solution_> getScoreDirector() {
        return externalScoreDirector;
    }

    /**
     * Allows for reading data produced by a temporary move, before it is undone.
     * The score argument represents the score after executing the move on the solution.
     * The move argument represents the undo move for that move.
     *
     * @param <Solution_> type of the solution
     * @param <Score_> score of the move
     * @param <Result_> user-defined return type of the function
     */
    @FunctionalInterface
    public interface TemporaryMovePostprocessor<Solution_, Score_ extends Score<Score_>, Result_>
            extends BiFunction<InnerScore<Score_>, Move<Solution_>, Result_> {

    }

}
