package ai.timefold.solver.core.preview.api.move;

import java.util.List;

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
 * <strong>This package and all of its contents are part of the Neighborhoods API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method, or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it,
 * before we finalize the API.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver GitHub</a>
 * or to <a href="https://discord.com/channels/1413420192213631086/1414521616955605003">Timefold Discord</a>.
 *
 * @param <Solution_>
 */
@NullMarked
public interface MutableSolutionView<Solution_> extends SolutionView<Solution_> {

    /**
     * As defined by {@link #assignValueAndAdd(PlanningListVariableMetaModel, Object, Object, int)}.
     * Will be removed right before this API is moved out of preview.
     *
     * @deprecated Use {@link #assignValueAndAdd(PlanningListVariableMetaModel, Object, Object, int)} instead.
     */
    @Deprecated(forRemoval = true)
    default <Entity_, Value_> void assignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value, Entity_ destinationEntity, int destinationIndex) {
        assignValueAndAdd(variableMetaModel, value, destinationEntity, destinationIndex);
    }

    /**
     * Puts a given value at a particular index in a given entity's {@link PlanningListVariable planning list variable}.
     * Moves all values at or after the index to the right, much like {@link List#add(int, Object)}.
     *
     * @param variableMetaModel Describes the variable to be changed.
     * @param value The value to be assigned to a list variable.
     * @param destinationEntity The entity whose list variable is to be changed.
     * @param destinationIndex The index in the list variable at which the value is to be assigned,
     *        moving the pre-existing value at that index and all subsequent values to the right.
     * @throws IllegalStateException if the value is already assigned to a list variable
     */
    <Entity_, Value_> void assignValueAndAdd(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value, Entity_ destinationEntity, int destinationIndex);

    /**
     * Puts a given value at a particular index in a given entity's {@link PlanningListVariable planning list variable}.
     * much like {@link List#set(int, Object)}.
     * The original value at that index becomes unassigned.
     * If the destination index is equal to the list size,
     * the value is appended to the end of the list without unassigning any value.
     *
     * @param variableMetaModel Describes the variable to be changed.
     * @param value The value to be assigned to a list variable.
     * @param destinationEntity The entity whose list variable is to be changed.
     * @param destinationIndex The index in the list variable at which the value is to be assigned,
     *        unassigning the pre-existing value at that index.
     * @throws IllegalStateException if the value is already assigned to a list variable
     */
    <Entity_, Value_> void assignValueAndSet(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value, Entity_ destinationEntity, int destinationIndex);

    /**
     * Removes a given value from the {@link PlanningListVariable planning list variable} that it's part of.
     * Shifts any subsequent values to the left.
     *
     * @param variableMetaModel Describes the variable to be changed.
     * @param value The value to be removed from a list variable.
     * @throws IllegalStateException if the value is not assigned to a list variable
     */
    <Entity_, Value_> void unassignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value);

    /**
     * Removes a value from a given entity's {@link PlanningListVariable planning list variable} at a given index.
     * Shifts any subsequent values to the left.
     *
     * @param variableMetaModel Describes the variable to be changed.
     * @param entity The entity whose element is to be removed from a list variable.
     * @param index The index in entity's list variable which contains the value to be removed;
     *        Acceptable values range from zero to one less than list size.
     *        All values after the index are shifted to the left.
     * @return the removed value
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    <Entity_, Value_> Value_ unassignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, int index);

    /**
     * Changes the value of a {@link PlanningVariable basic planning variable} of a given entity.
     *
     * @param variableMetaModel Describes the variable to be changed.
     * @param entity The entity whose variable value is to be changed.
     * @param newValue maybe null, if unassigning the variable
     */
    <Entity_, Value_> void changeVariable(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, @Nullable Value_ newValue);

    /**
     * Moves a value from one entity's {@link PlanningListVariable planning list variable} to another.
     * To move values within the same entity,
     * use {@link #moveValueInList(PlanningListVariableMetaModel, Object, int, int)} instead.
     *
     * @param variableMetaModel Describes the variable to be changed.
     * @param sourceEntity The entity from which the value will be removed.
     * @param sourceIndex The index in the source entity's list variable which contains the value to be moved;
     *        Acceptable values range from zero to one less than list size.
     *        All values after the index are shifted to the left.
     * @param destinationEntity The entity to which the value will be added.
     * @param destinationIndex The index in the destination entity's list variable to which the value will be moved;
     *        Acceptable values range from zero to equal to list size.
     *        All values at or after the index are shifted to the right.
     *        To append to the end of the list, use the list size as index.
     * @return the value that was moved
     * @throws IndexOutOfBoundsException if either index is out of bounds
     * @throws IllegalArgumentException if sourceEntity == destinationEntity
     */
    <Entity_, Value_> Value_ moveValueBetweenLists(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ sourceEntity, int sourceIndex, Entity_ destinationEntity, int destinationIndex);

    /**
     * Moves a value within one entity's {@link PlanningListVariable planning list variable}.
     * Behaves as if the value is first removed from the source index,
     * shifting all later values to the left,
     * and then inserted at the destination index.
     * This means that, to move the item to the end of the list,
     * the destination index must equal the list size minus one.
     *
     * @param variableMetaModel Describes the variable to be changed.
     * @param sourceEntity The entity whose variable value is to be changed.
     * @param sourceIndex The index in the source entity's list variable which contains the value to be moved;
     *        Acceptable values range from zero to one less than list size.
     *        All values after the index are shifted to the left.
     * @param destinationIndex The index in the source entity's list variable to which the value will be moved;
     *        Acceptable values range from zero to one less than list size.
     *        All values at or after the index are shifted to the right.
     * @return the value that was moved
     * @throws IndexOutOfBoundsException if either index is out of bounds
     * @throws IllegalArgumentException if sourceIndex == destinationIndex
     */
    <Entity_, Value_> Value_ moveValueInList(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ sourceEntity, int sourceIndex, int destinationIndex);

    /**
     * Swaps two values between two entities' {@link PlanningListVariable planning list variable}.
     *
     * @param variableMetaModel Describes the variable to be changed.
     * @param leftEntity The first entity whose variable value is to be swapped.
     * @param leftIndex The index in the left entity's list variable which contains the value to be swapped;
     *        Acceptable values range from zero to one less than list size.
     * @param rightEntity The second entity whose variable value is to be swapped.
     * @param rightIndex The index in the right entity's list variable which contains the other value to be swapped;
     *        Acceptable values range from zero to one less than list size.
     * @throws IndexOutOfBoundsException if either index is out of bounds
     * @throws IllegalArgumentException if leftEntity == rightEntity while leftIndex == rightIndex
     */
    <Entity_, Value_> void swapValuesBetweenLists(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ leftEntity, int leftIndex, Entity_ rightEntity, int rightIndex);

    /**
     * Swaps two values within one entity's {@link PlanningListVariable planning list variable}.
     *
     * @param variableMetaModel Describes the variable to be changed.
     * @param entity The entity whose variable values are to be swapped.
     * @param leftIndex The index in the entity's list variable which contains the value to be swapped;
     *        Acceptable values range from zero to one less than list size.
     * @param rightIndex The index in the entity's list variable which contains the other value to be swapped;
     *        Acceptable values range from zero to one less than list size.
     * @throws IndexOutOfBoundsException if either index is out of bounds
     * @throws IllegalArgumentException if leftIndex == rightIndex
     */
    <Entity_, Value_> void swapValuesInList(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, int leftIndex, int rightIndex);

}
