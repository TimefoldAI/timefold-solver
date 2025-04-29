package ai.timefold.solver.core.impl.move.director;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.LegacyMoveAdapter;
import ai.timefold.solver.core.impl.move.InnerMutableSolutionView;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public sealed class MoveDirector<Solution_, Score_ extends Score<Score_>>
        implements InnerMutableSolutionView<Solution_>, Rebaser
        permits EphemeralMoveDirector {

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
    public final <Entity_, Value_> void assignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ planningValue, Entity_ destinationEntity, int destinationIndex) {
        var variableDescriptor =
                ((DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
        externalScoreDirector.beforeListVariableElementAssigned(variableDescriptor, planningValue);
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex);
        variableDescriptor.addElement(destinationEntity, destinationIndex, planningValue);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex + 1);
        externalScoreDirector.afterListVariableElementAssigned(variableDescriptor, planningValue);
    }

    @Override
    public final <Entity_, Value_> void unassignValue(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ movedValue, Entity_ sourceEntity, int sourceIndex) {
        var variableDescriptor =
                ((DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
        externalScoreDirector.beforeListVariableElementUnassigned(variableDescriptor, movedValue);
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex + 1);
        variableDescriptor.getValue(sourceEntity).remove(sourceIndex);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex);
        externalScoreDirector.afterListVariableElementUnassigned(variableDescriptor, movedValue);
    }

    public final <Entity_, Value_> void changeVariable(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, @Nullable Value_ newValue) {
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        externalScoreDirector.beforeVariableChanged(variableDescriptor, entity);
        variableDescriptor.setValue(entity, newValue);
        externalScoreDirector.afterVariableChanged(variableDescriptor, entity);
    }

    @SuppressWarnings("unchecked")
    public final <Entity_, Value_> @Nullable Value_ moveValueBetweenLists(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ sourceEntity, int sourceIndex,
            Entity_ destinationEntity, int destinationIndex) {
        if (sourceEntity == destinationEntity) {
            return moveValueInList(variableMetaModel, sourceEntity, sourceIndex, destinationIndex);
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
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, entity, sourceIndex, toIndex);
        var variable = (List<Value_>) variableDescriptor.getValue(entity);
        var value = variable.remove(sourceIndex);
        variable.add(destinationIndex, value);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, entity, sourceIndex, toIndex);
        return value;
    }

    /**
     * Execute a given move and make sure shadow variables are up to date after that.
     */
    public final void execute(Move<Solution_> move) {
        move.execute(this);
        externalScoreDirector.triggerVariableListeners();
    }

    // Only used in tests of legacy moves.
    public final void execute(ai.timefold.solver.core.impl.heuristic.move.Move<Solution_> move) {
        execute(new LegacyMoveAdapter<>(move));
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

    // Only used in tests of legacy moves.
    public final <Result_> Result_ executeTemporary(ai.timefold.solver.core.impl.heuristic.move.Move<Solution_> move,
            TemporaryMovePostprocessor<Solution_, Score_, Result_> postprocessor) {
        return executeTemporary(new LegacyMoveAdapter<>(move), postprocessor);
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
    public <Entity_, Value_> ElementPosition
            getPositionOf(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value) {
        return getPositionOf(backingScoreDirector, variableMetaModel, value);
    }

    protected static <Solution_, Entity_, Value_> ElementPosition getPositionOf(InnerScoreDirector<Solution_, ?> scoreDirector,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value) {
        return scoreDirector.getListVariableStateSupply(extractVariableDescriptor(variableMetaModel))
                .getElementPosition(value);
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
