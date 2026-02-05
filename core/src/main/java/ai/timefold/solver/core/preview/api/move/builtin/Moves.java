package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.List;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Factory class for creating built-in {@link Move} instances that mutate planning variables.
 * <p>
 * This class provides static methods to create the standard moves used in optimization:
 * <ul>
 * <li>{@link #compose(Move[])} - combines multiple moves into one</li>
 * <li>{@link #change(PlanningVariableMetaModel, Object, Object)} - changes a basic planning variable's value</li>
 * <li>{@link #swap(PlanningVariableMetaModel, Object, Object)} - swaps values between two entities</li>
 * <li>{@link #assign(PlanningListVariableMetaModel, Object, Object, int)} - assigns a value to a list variable</li>
 * <li>{@link #unassign(PlanningListVariableMetaModel, Object, int)} - removes a value from a list variable</li>
 * <li>{@link #change(PlanningListVariableMetaModel, PositionInList, PositionInList)} - moves an element within or between list
 * variables</li>
 * <li>{@link #swap(PlanningListVariableMetaModel, PositionInList, PositionInList)} - swaps two elements in list variables</li>
 * </ul>
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
 * @see MutableSolutionView The view used by moves to perform mutating operations.
 */
@NullMarked
public final class Moves {

    /**
     * Creates a composite move from a list of moves.
     * <p>
     * When executed, the composite move executes all its child moves in order.
     * If the list contains only one move, that move is returned directly without wrapping.
     *
     * @param moves the list of moves to combine; must not be empty
     * @param <Solution_> the solution type
     * @return a single move that executes all the given moves, or the single move if the list contains only one
     * @throws UnsupportedOperationException if the list is empty
     */
    @SuppressWarnings("unchecked")
    public static <Solution_> Move<Solution_> compose(List<Move<Solution_>> moves) {
        return compose(moves.toArray(new Move[0]));
    }

    /**
     * Creates a composite move from an array of moves.
     * <p>
     * When executed, the composite move executes all its child moves in order.
     * If the array contains only one move, that move is returned directly without wrapping.
     *
     * @param moves the array of moves to combine; must not be empty
     * @param <Solution_> the solution type
     * @return a single move that executes all the given moves, or the single move if the array contains only one
     * @throws UnsupportedOperationException if the array is empty
     */
    @SafeVarargs
    public static <Solution_> Move<Solution_> compose(Move<Solution_>... moves) {
        return CompositeMove.buildMove(moves);
    }

    // ************************************************************************
    // Basic variable moves
    // ************************************************************************

    /**
     * Creates a move that changes a basic planning variable's value on a given entity.
     * <p>
     * This move is the fundamental building block for optimizing basic planning variables.
     * It sets the variable on the entity to a new value.
     *
     * @param variableMetaModel describes the planning variable to be changed
     * @param entity the entity whose variable value is to be changed
     * @param value the new value to assign; may be null if the variable supports unassigned values
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable value type
     * @return a move that, when executed, changes the entity's variable to the given value
     */
    public static <Solution_, Entity_, Value_> Move<Solution_> change(
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity, @Nullable Value_ value) {
        return new ChangeMove<>(variableMetaModel, entity, value);
    }

    /**
     * Creates a move that swaps the value of a single planning variable between two entities.
     * <p>
     * Both entities must be different instances. After execution, the left entity will have
     * the value that the right entity had, and vice versa.
     * <p>
     * Only provide entities whose values can be swapped;
     * for example, if one of the values is not in the value range of the other entity's variable,
     * then swapping would lead to an invalid solution.
     *
     * @param variableMetaModel describes the planning variable to swap
     * @param leftEntity the first entity participating in the swap
     * @param rightEntity the second entity participating in the swap
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable value type
     * @return a move that, when executed, swaps the variable values between the two entities
     * @throws IllegalArgumentException if leftEntity == rightEntity
     */
    @SuppressWarnings("unchecked")
    public static <Solution_, Entity_, Value_> Move<Solution_> swap(
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ leftEntity, Entity_ rightEntity) {
        return swap(List.of((PlanningVariableMetaModel<Solution_, Entity_, Object>) variableMetaModel), leftEntity,
                rightEntity);
    }

    /**
     * Creates a move that swaps the values of multiple planning variables between two entities.
     * <p>
     * All variables in the list must belong to the same entity class.
     * Both entities must be different instances. For each variable in the list, after execution,
     * the left entity will have the value that the right entity had, and vice versa.
     * <p>
     * Only provide entities whose values can be swapped;
     * for example, if one of the values is not in the value range of the other entity's variable,
     * then swapping would lead to an invalid solution.
     *
     * @param variableMetaModelList the list of planning variables to swap; must not be empty
     * @param leftEntity the first entity participating in the swap
     * @param rightEntity the second entity participating in the swap
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @return a move that, when executed, swaps all variable values between the two entities
     * @throws IllegalArgumentException if the list is empty or if leftEntity == rightEntity
     */
    public static <Solution_, Entity_> Move<Solution_> swap(
            List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList, Entity_ leftEntity,
            Entity_ rightEntity) {
        return new SwapMove<>(variableMetaModelList, leftEntity, rightEntity);
    }

    // ************************************************************************
    // List variable moves
    // ************************************************************************

    /**
     * Creates a move that assigns a value to a list variable at a specified position.
     * <p>
     * The value must not already be assigned to any list variable.
     * This move inserts the value at the given position, shifting all existing values
     * at or after that position to the right.
     *
     * @param variableMetaModel describes the list variable to be changed
     * @param value the value to be assigned; must not already be assigned to a list variable
     * @param targetPosition specifies the entity and index where the value should be inserted
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable value type
     * @return a move that, when executed, assigns the value to the list variable at the specified position
     */
    public static <Solution_, Entity_, Value_> Move<Solution_> assign(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value,
            PositionInList targetPosition) {
        return assign(variableMetaModel, value, targetPosition.entity(), targetPosition.index());
    }

    /**
     * As defined by {@link #assign(PlanningListVariableMetaModel, Object, PositionInList)},
     * but with explicit entity and index parameters.
     */
    public static <Solution_, Entity_, Value_> Move<Solution_> assign(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value, Entity_ entity,
            int index) {
        return new ListAssignMove<>(variableMetaModel, value, entity, index);
    }

    /**
     * Creates a move that unassigns a value from a list variable at a specified position.
     * <p>
     * This move removes the value at the given position, shifting all subsequent values to the left.
     * After execution, the removed value will be unassigned.
     *
     * @param variableMetaModel describes the list variable to be changed
     * @param targetPosition specifies the entity and index from which the value should be removed
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable value type
     * @return a move that, when executed, removes the value from the list variable
     */
    public static <Solution_, Entity_, Value_> Move<Solution_> unassign(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            PositionInList targetPosition) {
        return unassign(variableMetaModel, targetPosition.entity(), targetPosition.index());
    }

    /**
     * As defined by {@link #unassign(PlanningListVariableMetaModel, PositionInList)},
     * but with explicit entity and index parameters.
     */
    public static <Solution_, Entity_, Value_> Move<Solution_> unassign(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity,
            int index) {
        return new ListUnassignMove<>(variableMetaModel, entity, index);
    }

    /**
     * Creates a move that moves an element from one position in a list variable to another position.
     * <p>
     * The element at the left position is removed and inserted at the right position.
     * The left and right positions may be in the same or different entities.
     * <p>
     * If the source and destination are within the same entity, the element is first removed
     * from the source index (shifting later elements left), then inserted at the destination index.
     * This means that if the destination index is after the source index,
     * the user should decrement it by one to account for the shift.
     *
     * @param variableMetaModel describes the list variable to be changed
     * @param left the source position from which to move the element
     * @param right the destination position to which to move the element
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable value type
     * @return a move that, when executed, relocates the element from the left position to the right position
     */
    public static <Solution_, Entity_, Value_> Move<Solution_> change(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, PositionInList left,
            PositionInList right) {
        return change(variableMetaModel, left.entity(), left.index(), right.entity(), right.index());
    }

    /**
     * As defined by {@link #change(PlanningListVariableMetaModel, PositionInList, PositionInList)},
     * but with explicit entity and index parameters.
     */
    public static <Solution_, Entity_, Value_> Move<Solution_> change(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ leftEntity, int leftIndex,
            Entity_ rightEntity, int rightIndex) {
        return new ListChangeMove<>(variableMetaModel, leftEntity, leftIndex, rightEntity, rightIndex);
    }

    /**
     * Creates a move that swaps two elements between positions in list variables.
     * <p>
     * The element at the left position is swapped with the element at the right position.
     * The left and right positions may be in the same or different entities.
     *
     * @param variableMetaModel describes the list variable to be changed
     * @param left the first position for the swap
     * @param right the second position for the swap
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable value type
     * @return a move that, when executed, swaps the elements at the two positions
     */
    public static <Solution_, Entity_, Value_> Move<Solution_> swap(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, PositionInList left,
            PositionInList right) {
        return swap(variableMetaModel, left.entity(), left.index(), right.entity(), right.index());
    }

    /**
     * As defined by {@link #swap(PlanningListVariableMetaModel, PositionInList, PositionInList)},
     * but with explicit entity and index parameters.
     */
    public static <Solution_, Entity_, Value_> Move<Solution_> swap(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ leftEntity, int leftIndex,
            Entity_ rightEntity, int rightIndex) {
        return new ListSwapMove<>(variableMetaModel, leftEntity, leftIndex, rightEntity, rightIndex);
    }

    private Moves() {
        // No external instances.
    }

}
