package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.impl.move.builtin.ChangeMove;
import ai.timefold.solver.core.impl.move.builtin.ListAssignMove;
import ai.timefold.solver.core.impl.move.builtin.ListChangeMove;
import ai.timefold.solver.core.impl.move.builtin.ListSwapMove;
import ai.timefold.solver.core.impl.move.builtin.ListUnassignMove;
import ai.timefold.solver.core.impl.move.builtin.MassChangeMove;
import ai.timefold.solver.core.impl.move.builtin.MassListChangeMove;
import ai.timefold.solver.core.impl.move.builtin.PillarSwapMove;
import ai.timefold.solver.core.impl.move.builtin.SubListChangeMove;
import ai.timefold.solver.core.impl.move.builtin.SubListSwapMove;
import ai.timefold.solver.core.impl.move.builtin.SubListUnassignMove;
import ai.timefold.solver.core.impl.move.builtin.SwapMove;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Factory class for creating built-in {@link Move} instances that mutate planning variables.
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
        return CompositeMove.buildMove(Arrays.asList(moves));
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
     * Both entities must be different instances.
     * After execution, the left entity will have the value that the right entity had, and vice versa.
     * <p>
     * The caller MUST only provide entities whose values can be swapped;
     * for example, if one of the values is not in the value range of the other entity's variable,
     * swapping would lead to an invalid solution.
     * This is not re-checked by the move;
     * see {@link SwapMove} for what happens when a caller violates it.
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
     * Both entities must be different instances.
     * For each variable in the list, after execution, the left entity will have the value that the right entity had, and vice
     * versa.
     * <p>
     * The caller MUST only provide entities whose values can be swapped;
     * for example, if one of the values is not in the value range of the other entity's variable,
     * swapping would lead to an invalid solution.
     * This is not re-checked by the move;
     * see {@link SwapMove} for what happens when a caller violates it.
     *
     * @param variableMetaModelList the list of planning variables to swap; must not be empty.
     *        Keep the variableMetaModelList list in stable order,
     *        otherwise move equality will misbehave;
     *        the generic move providers guarantee that.
     * @param leftEntity the first entity participating in the swap
     * @param rightEntity the second entity participating in the swap
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @return a move that, when executed, swaps all variable values between the two entities
     * @throws IllegalArgumentException if the list is empty or if leftEntity == rightEntity
     */
    @SuppressWarnings("unchecked")
    public static <Solution_, Entity_> Move<Solution_> swap(
            List<? extends PlanningVariableMetaModel<Solution_, Entity_, ?>> variableMetaModelList, Entity_ leftEntity,
            Entity_ rightEntity) {
        return new SwapMove<>((List<PlanningVariableMetaModel<Solution_, Entity_, Object>>) variableMetaModelList, leftEntity,
                rightEntity);
    }

    /**
     * Creates a move that changes a basic planning variable's value on every member of a {@link Sample} at once.
     * <p>
     * This is the sample equivalent of {@link #change(PlanningVariableMetaModel, Object, Object)}:
     * an assign is a move whose members currently hold null, and an unassign is a move whose destination is null.
     *
     * @param variableMetaModel describes the planning variable to be changed
     * @param sample the sample whose members' variable value is to be changed
     * @param toPlanningValue the new value to assign; may be null if the variable supports unassigned values
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable value type
     * @return a move that, when executed, changes every member's variable to the given value
     */
    public static <Solution_, Entity_, Value_> Move<Solution_> massChange(
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Sample<Entity_> sample,
            @Nullable Value_ toPlanningValue) {
        return new MassChangeMove<>(variableMetaModel, sample, toPlanningValue);
    }

    /**
     * As defined by {@link #pillarSwap(List, Sample, Sample)}, but for a single variable.
     */
    @SuppressWarnings("unchecked")
    public static <Solution_, Entity_> Move<Solution_> pillarSwap(
            PlanningVariableMetaModel<Solution_, Entity_, ?> variableMetaModel, Sample<Entity_> leftPillar,
            Sample<Entity_> rightPillar) {
        return pillarSwap(List.of((PlanningVariableMetaModel<Solution_, Entity_, Object>) variableMetaModel), leftPillar,
                rightPillar);
    }

    /**
     * Creates a move that swaps the values of one or more planning variables between the members of two {@link Sample}s.
     * <p>
     * This is the pillar equivalent of {@link #swap(List, Object, Object)}.
     * The caller MUST only pass pillars that do not share any members,
     * homogeneous pillars (every member of a pillar holding the same value for each listed variable),
     * and pillars whose values can be swapped;
     * for example, if one of the values is not in the value range of a member of the other pillar,
     * swapping would lead to an invalid solution.
     * Neither condition is re-checked by the move;
     * see {@link PillarSwapMove} for what happens when a caller violates them.
     *
     * @param variableMetaModelList the list of planning variables to swap; must not be empty.
     *        Keep the variableMetaModelList list in stable order,
     *        otherwise move equality will misbehave;
     *        the generic move providers guarantee that.
     * @param leftPillar the first pillar participating in the swap
     * @param rightPillar the second pillar participating in the swap
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @return a move that, when executed, swaps all variable values between the two pillars
     * @throws IllegalArgumentException if the list is empty
     */
    @SuppressWarnings("unchecked")
    public static <Solution_, Entity_> Move<Solution_> pillarSwap(
            List<? extends PlanningVariableMetaModel<Solution_, Entity_, ?>> variableMetaModelList,
            Sample<Entity_> leftPillar, Sample<Entity_> rightPillar) {
        return new PillarSwapMove<>((List<PlanningVariableMetaModel<Solution_, Entity_, Object>>) variableMetaModelList,
                leftPillar, rightPillar);
    }

    // ************************************************************************
    // List variable moves
    // ************************************************************************

    /**
     * Creates a move that assigns a value to a list variable at a specified position.
     * <p>
     * The value must not already be assigned to any list variable.
     * This move inserts the value at the given position,
     * shifting all existing values at or after that position to the right.
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
     * The element at the source position is removed and inserted at the destination position.
     * Both positions may be in the same entity or in different entities.
     * <p>
     * If the source and destination are within the same entity,
     * the element is first removed from the source position (shifting later elements left), then inserted at the destination
     * position.
     *
     * @param variableMetaModel describes the list variable to be changed
     * @param source the source position from which to move the element
     * @param destination the destination position to which to move the element
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable value type
     * @return a move that, when executed, relocates the element from the source position to the destination position
     */
    public static <Solution_, Entity_, Value_> Move<Solution_> change(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, PositionInList source,
            PositionInList destination) {
        return change(variableMetaModel, source.entity(), source.index(), destination.entity(), destination.index());
    }

    /**
     * As defined by {@link #change(PlanningListVariableMetaModel, PositionInList, PositionInList)},
     * but with explicit entity and index parameters.
     */
    public static <Solution_, Entity_, Value_> Move<Solution_> change(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ sourceEntity, int sourceIndex,
            Entity_ destinationEntity, int destinationIndex) {
        return new ListChangeMove<>(variableMetaModel, sourceEntity, sourceIndex, destinationEntity, destinationIndex);
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

    /**
     * Creates a move that relocates a contiguous span of a list variable to a different position,
     * possibly on a different entity.
     * <p>
     * The span is identified by a {@link Range}.
     * It is first removed from its source position (shifting later elements left),
     * then inserted at the destination position,
     * optionally in reverse element order.
     * <p>
     * Neither overlap between the span and the destination,
     * nor destination value-range legality,
     * is checked by this move;
     * that is the caller's responsibility.
     *
     * @param variableMetaModel describes the list variable to be changed
     * @param source the span to move
     * @param destination the destination position at which to insert the span
     * @param reversing if {@code true}, the span is inserted in reverse element order
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable value type
     * @return a move that, when executed, relocates the span to the destination position
     */
    public static <Solution_, Entity_, Value_> Move<Solution_> change(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Range<Entity_> source,
            PositionInList destination, boolean reversing) {
        return new SubListChangeMove<>(variableMetaModel, source, destination, reversing);
    }

    /**
     * Creates a move that swaps two contiguous spans of a list variable,
     * possibly on different entities.
     * <p>
     * Each span is identified by a {@link Range}.
     * When both spans are on the same entity, they must not overlap;
     * this move does not check this.
     *
     * @param variableMetaModel describes the list variable to be changed
     * @param left the first span participating in the swap
     * @param right the second span participating in the swap
     * @param reversing if {@code true}, both spans are inserted in reverse element order
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable value type
     * @return a move that, when executed, swaps the two spans
     */
    public static <Solution_, Entity_, Value_> Move<Solution_> swap(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Range<Entity_> left,
            Range<Entity_> right, boolean reversing) {
        return new SubListSwapMove<>(variableMetaModel, left, right, reversing);
    }

    /**
     * Creates a move that reverses a contiguous span of a list variable in place.
     * This is the classic 2-opt route-improving move.
     * <p>
     * The span is identified by a {@link Range}.
     *
     * @param variableMetaModel describes the list variable to be changed
     * @param range the span to reverse; its length must be at least 2
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable value type
     * @return a move that, when executed, reverses the span in place
     * @throws IllegalArgumentException if the range's length is less than 2
     */
    public static <Solution_, Entity_, Value_> Move<Solution_> reverse(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Range<Entity_> range) {
        if (range.length() < 2) {
            throw new IllegalArgumentException("The length (%d) of range (%s) must be at least 2."
                    .formatted(range.length(), range));
        }
        return new SubListChangeMove<>(variableMetaModel, range,
                ElementPosition.of(range.entity(), range.fromIndex()), true);
    }

    /**
     * Creates a move that unassigns a contiguous span of a list variable,
     * that is, removes every value of the span from the list, leaving it unassigned.
     *
     * @param variableMetaModel describes the list variable to be changed
     * @param range the span to unassign
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable value type
     * @return a move that, when executed, removes every value of the span from the list variable
     */
    public static <Solution_, Entity_, Value_> Move<Solution_> unassign(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Range<Entity_> range) {
        return new SubListUnassignMove<>(variableMetaModel, range);
    }

    /**
     * Creates a move that inserts every member of a {@link Sample} of a list variable consecutively,
     * in sample iteration order, at one destination position.
     * <p>
     * This is the list equivalent of {@link #massChange(PlanningVariableMetaModel, Sample, Object)}:
     * an assign is a move whose members currently hold no position,
     * and an unassign is a move whose destination is {@code null}.
     *
     * @param variableMetaModel describes the list variable to be changed
     * @param sample the sample whose members are to be gathered
     * @param destination the destination position at which to insert every member;
     *        {@code null} unassigns every member instead
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable value type
     * @return a move that, when executed, gathers every member at the destination position, or unassigns them all
     */
    public static <Solution_, Entity_, Value_> Move<Solution_> massChange(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Sample<Value_> sample,
            @Nullable PositionInList destination) {
        return new MassListChangeMove<>(variableMetaModel, sample, destination);
    }

    private Moves() {
        // No external instances.
    }

}
