package ai.timefold.solver.core.preview.api.move;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Contains all reading and mutating methods available to a {@link Move}
 * in order to change the state of a {@link PlanningSolution planning solution}.
 * <p>
 * <strong>This package and all of its contents are part of the Move Streams API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method, or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it,
 * before we finalize the API.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver Github</a>.
 * 
 * @param <Solution_>
 */
@NullMarked
public interface MutableSolutionView<Solution_> extends SolutionView<Solution_> {

    /**
     * Puts a given value at a particular index in a given entity's {@link PlanningListVariable planning list variable}.
     * Moves all values at or after the index to the right.
     *
     * @param variableMetaModel Describes the variable to be changed.
     * @param value The value to be assigned to a list variable.
     * @param destinationEntity The entity whose list variable is to be changed.
     * @param destinationIndex The index at which the value is to be assigned.
     */
    <Entity_, Value_> void assignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value, Entity_ destinationEntity, int destinationIndex);

    /**
     * Removes a given value from the {@link PlanningListVariable planning list variable} that it's part of.
     * Shifts any later values to the left.
     *
     * @param variableMetaModel Describes the variable to be changed.
     * @param value The value to be removed from a list variable.
     * @throws IllegalStateException if the value is not assigned to a list variable
     */
    default <Entity_, Value_> void unassignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value) {
        var locationInList = getPositionOf(variableMetaModel, value)
                .ensureAssigned(() -> """
                        The value (%s) is not assigned to a list variable.
                        This may indicate score corruption or a problem with the move's implementation."""
                        .formatted(value));
        unassignValue(variableMetaModel, value, locationInList.entity(), locationInList.index());
    }

    /**
     * Removes a value from a given entity's {@link PlanningListVariable planning list variable} at a given index.
     * Shifts any later values to the left.
     *
     * @param variableMetaModel Describes the variable to be changed.
     * @param entity The entity whose element is to be removed from a list variable.
     * @param index >= 0
     * @return the removed value
     * @throws IllegalArgumentException if the index is out of bounds
     */
    default <Entity_, Value_> Value_ unassignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, int index) {
        var value = getValueAtIndex(variableMetaModel, entity, index);
        unassignValue(variableMetaModel, value, entity, index);
        return value;
    }

    /**
     * Removes a given value from a given entity's {@link PlanningListVariable planning list variable} at a given index.
     * Shifts any later values to the left.
     *
     * @param variableMetaModel Describes the variable to be changed.
     * @param value The value to be unassigned from a list variable.
     * @param entity The entity whose value is to be unassigned from a list variable.
     * @param index >= 0
     * @throws IllegalArgumentException if the index is out of bounds
     * @throws IllegalStateException if the actual value at the given index is not the given value
     */
    <Entity_, Value_> void unassignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value, Entity_ entity, int index);

    /**
     * Reads the value of a @{@link PlanningVariable basic planning variable} of a given entity.
     * 
     * @param variableMetaModel Describes the variable to be changed.
     * @param entity The entity whose variable value is to be changed.
     * @param newValue maybe null, if unassigning the variable
     */
    <Entity_, Value_> void changeVariable(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, @Nullable Value_ newValue);

    /**
     * Moves a value from one entity's {@link PlanningListVariable planning list variable} to another.
     *
     * @param variableMetaModel Describes the variable to be changed.
     * @param sourceEntity The first entity whose variable value is to be changed.
     * @param sourceIndex >= 0
     * @param destinationEntity The second entity whose variable value is to be changed.
     * @param destinationIndex >= 0
     * @return the value that was moved; null if nothing was moved
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    <Entity_, Value_> @Nullable Value_ moveValueBetweenLists(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ sourceEntity, int sourceIndex,
            Entity_ destinationEntity, int destinationIndex);

    /**
     * Moves a value within one entity's {@link PlanningListVariable planning list variable}.
     *
     * @param variableMetaModel Describes the variable to be changed.
     * @param entity The entity whose variable value is to be changed.
     * @param sourceIndex >= 0
     * @param destinationIndex >= 0
     * @return the value that was moved; null if nothing was moved
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    <Entity_, Value_> @Nullable Value_ moveValueInList(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity, int sourceIndex,
            int destinationIndex);

}
