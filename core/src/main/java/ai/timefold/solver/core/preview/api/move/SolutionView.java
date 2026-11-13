package ai.timefold.solver.core.preview.api.move;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.entity.PlanningPinToIndex;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Allows read-only access to the state of the solution that is being operated on by the {@link Move}.
 * <p>
 * <strong>This package and all of its contents are part of the Neighborhoods API, which is under development and is only
 * offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility; any class, method, or field may change or be removed without prior
 * notice, although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it, before we finalize the API.
 * Please direct your feedback to <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver GitHub</a>
 * or to <a href="https://discord.com/channels/1413420192213631086/1414521616955605003">Timefold Discord</a>.
 *
 * @param <Solution_>
 */
@NullMarked
public interface SolutionView<Solution_> {

    /**
     * Gets the meta-model of the solution, which provides access to the structure of the solution and its variables.
     *
     * @return the meta-model of the solution
     */
    PlanningSolutionMetaModel<Solution_> getSolutionMetaModel();

    /**
     * Reads the value of a {@link PlanningVariable basic planning variable} of a given entity.
     *
     * @param variableMetaModel Describes the variable whose value is to be read.
     * @param entity The entity whose variable is to be read.
     * @return The value of the variable on the entity.
     */
    <Entity_, Value_> @Nullable Value_
            getValue(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity);

    /**
     * Reads the value of a {@link PlanningListVariable list planning variable} and returns its length.
     *
     * @param variableMetaModel Describes the variable whose value is to be read.
     * @param entity The entity whose variable is to be read.
     * @return The number of values in the list variable.
     * @throws NullPointerException if the value of the list variable is null.
     */
    <Entity_, Value_> int countValues(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity);

    /**
     * Reads the value of a {@link PlanningListVariable list planning variable} of a given entity at a specific index.
     *
     * @param variableMetaModel Describes the variable whose value is to be read.
     * @param entity The entity whose variable is to be read.
     * @param index >= 0
     * @return The value at the given index in the list variable, valid only at the time of this call; it does not update if the
     *         list variable changes afterward.
     * @throws NullPointerException if the value of the list variable is null.
     * @throws IndexOutOfBoundsException if the index is out of bounds.
     */
    <Entity_, Value_> Value_ getValueAtIndex(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, int index);

    /**
     * Checks if a given value is assigned in any {@link PlanningListVariable list planning variable}.
     * A possibly more efficient variant of {@link #getPositionOf(PlanningListVariableMetaModel, Object)},
     * in case you don't need the actual position.
     *
     * @param variableMetaModel Describes the variable whose value is to be read.
     * @param value The value to locate.
     * @return true if the value is assigned in any list variable.
     */
    <Entity_, Value_> boolean isAssigned(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value);

    /**
     * Locates a given value in any {@link PlanningListVariable list planning variable}.
     *
     * @param variableMetaModel Describes the variable whose value is to be read.
     * @param value The value to locate.
     * @return the location of the value in the variable, valid only at the time of this call; it does not update if the list
     *         variable changes afterward.
     */
    <Entity_, Value_> ElementPosition getPositionOf(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value);

    /**
     * Locates a given value in any {@link PlanningListVariable list planning variable}.
     * A possibly more efficient variant of {@link #getPositionOf(PlanningListVariableMetaModel, Object)},
     * in case you don't need the actual position.
     *
     * @param variableMetaModel Describes the variable whose value is to be read.
     * @param value The value to locate.
     * @return the entity whose list variable carries this value, or null if unassigned
     */
    <Entity_, Value_> @Nullable Entity_ getEntity(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value);

    /**
     * Reads the index of the first element of a {@link PlanningListVariable list planning variable} that is not pinned.
     * The pinned portion of a list variable, if any, is always a prefix: every index below the returned value is pinned, every
     * index at or above it is not.
     * <p>
     * <strong>Caveat:</strong> for an entity that is entirely immovable due to {@link PlanningPin}, this method returns
     * {@code 0} even though every element of its list is pinned; such an entity is excluded from the pinning-filtered
     * enumeration methods on {@link MoveStreamFactory},
     * so a caller which only ever enumerates through those methods will never see this case.
     *
     * @param variableMetaModel Describes the variable whose value is to be read.
     * @param entity The entity whose variable is to be read.
     * @return 0 or higher; 0 if nothing is pinned; at most {@link #countValues} if the entire list is pinned.
     */
    <Entity_, Value_> int getFirstUnpinnedIndex(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity);

    /**
     * Checks if a {@link PlanningEntity} with a basic {@link PlanningVariable} is pinned.
     *
     * @param variableMetaModel Describes the variable whose value is to be read.
     * @param entity The entity to check if it is pinned.
     * @return boolean indicating if the value is pinned in the variable.
     */
    <Entity_, Value_> boolean isPinned(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @Nullable Entity_ entity);

    /**
     * Checking if a {@link PlanningListVariable}'s value is pinned requires checking:
     * <ul>
     * <li>the entity's {@link PlanningPin} field,</li>
     * <li>the entity's {@link PlanningPinToIndex} field,</li>
     * <li>and the value's position in the list variable.</li>
     * </ul>
     * As this is complex, this method is provided as a convenience.
     *
     * @param variableMetaModel Describes the variable whose value is to be read.
     * @param value The value to check if it is pinned; may be null,
     *        in which case the method returns false.
     * @return boolean indicating if the value is pinned in the variable.
     */
    <Entity_, Value_> boolean isPinned(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @Nullable Value_ value);

    /**
     * Checks if a given value is present in the value range of a genuine planning variable, when the value range is defined on
     * {@link PlanningSolution}.
     *
     * @param variableMetaModel variable in question.
     * @param value value to check.
     * @param <Entity_> generic type of the entity that the variable is defined on.
     * @param <Value_> generic type of the value that the variable can take.
     * @return true if the value is acceptable for the variable.
     * @throws IllegalArgumentException if the value range is on an entity as opposed to a solution; use
     *         {@link #isValueInRange(GenuineVariableMetaModel, Object, Object)} to provide the entity instance.
     */
    default <Entity_, Value_> boolean isValueInRange(GenuineVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @Nullable Value_ value) {
        if (!variableMetaModel.isValueRangeOnSolution()) {
            throw new IllegalArgumentException("""
                    The variableMetaModel (%s) has a value range on an entity, not on the solution.
                    Maybe use isValueInRange(GenuineVariableMetaModel, Object, Object) to provide the entity instance."""
                    .formatted(variableMetaModel));
        }
        return isValueInRange(variableMetaModel, null, value);
    }

    /**
     * Checks if a given value is present in the value range of a genuine planning variable.
     * If the value range is defined on {@link PlanningEntity entity}, the {@code entity} argument must not be null.
     *
     * @param variableMetaModel variable in question.
     * @param entity entity that the value would be applied to; must be of a type that the variable is defined on.
     * @param value value to check.
     * @param <Entity_> generic type of the entity that the variable is defined on.
     * @param <Value_> generic type of the value that the variable can take.
     * @return true if the value is acceptable for the variable.
     */
    default <Entity_, Value_> boolean isValueInRange(GenuineVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @Nullable Entity_ entity, @Nullable Value_ value) {
        return getValueRange(variableMetaModel, entity)
                .contains(value);
    }

    /**
     * Returns the value range of a genuine planning variable, as it applies to the given entity.
     * If the value range is defined on {@link PlanningSolution} rather than on the entity, the same range is returned
     * regardless of which entity is given.
     * <p>
     * The returned range may include {@code null} if the variable {@link PlanningVariable#allowsUnassigned() allows unassigned}
     * values; use {@link ValueRange#contains(Object)} to check {@code null} the same way as any other value.
     *
     * @param variableMetaModel variable in question.
     * @param entity entity that the value would be applied to; must be of a type that the variable is defined on; may be null
     *        only if the value range is defined on {@link PlanningSolution}.
     * @param <Entity_> generic type of the entity that the variable is defined on.
     * @param <Value_> generic type of the value that the variable can take.
     * @return the value range of the variable.
     * @throws IllegalArgumentException if {@code entity} is null and the value range is defined on the entity, not on
     *         {@link PlanningSolution}.
     */
    <Entity_, Value_> ValueRange<Value_> getValueRange(GenuineVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @Nullable Entity_ entity);

    /**
     * Returns the movable entities whose value range of a genuine planning variable contains the given value.
     * Use it to draw only valid candidates for a move,
     * instead of drawing from all entities and rejecting those that cannot take the value;
     * the latter is slow when the value ranges are narrow.
     * <p>
     * Pinned entities ({@link PlanningPin}) are never included.
     * Entities pinned only up to an index ({@link PlanningPinToIndex}) are included.
     * If the value range is defined on {@link PlanningSolution},
     * every movable entity is returned if the range contains the value, and none if it does not.
     * The returned list contains only instances of the variable meta model's entity class (and its subclasses),
     * also if the variable is inherited from a superclass.
     * The returned list may contain the entity which currently has the value assigned.
     * The result does not depend on the current values of the entities,
     * so it can contain entities whose variable is unassigned or whose list is empty.
     * <p>
     * Do not keep the list beyond the current move iterator; it is invalid after a problem change.
     * For value ranges defined on an entity, the first call for a variable builds an index
     * whose size is proportional to the number of values times the number of entities.
     *
     * @param variableMetaModel variable in question.
     * @param value value to look up.
     * @param <Entity_> generic type of the variable meta model's entity;
     *        can be a subclass of the class that declares the variable.
     * @param <Value_> generic type of the value that the variable can take.
     * @return unmodifiable, with fast random access; empty if no movable entity accepts the value.
     */
    <Entity_, Value_> List<Entity_> getEntitiesWithValueInRange(
            GenuineVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value);

    /**
     * Returns the values which are in at least one entity's value range of a list variable together with the given value.
     * Every movable value which can share an entity's list with the given value is in this list.
     * It can also contain values which share a value range only with a pinned entity,
     * so that no movable entity can hold both.
     * Use it together with {@link #getEntitiesWithValueInRange(GenuineVariableMetaModel, Object)}
     * to draw only candidate positions or partners for the given value,
     * instead of drawing from all values and rejecting those whose entity cannot take the value.
     * <p>
     * Pinned values are never included: those of a pinned entity ({@link PlanningPin})
     * and those before an entity's {@link PlanningPinToIndex}.
     * The result does not depend on which values are currently assigned, so it can contain unassigned values;
     * check the entity of each value before using it.
     * If the value range is defined on {@link PlanningSolution},
     * every movable value of the range is returned if the range contains the given value, and none if it does not;
     * in that case, the list may contain the given value itself.
     * <p>
     * Do not keep the list beyond the current move iterator; it is invalid after a problem change.
     * For value ranges defined on an entity, the first call for a variable builds an index
     * whose size is proportional to the number of values times the number of values and entities.
     *
     * @param variableMetaModel variable in question.
     * @param value value to look up.
     * @param <Entity_> generic type of the entity that the variable is defined on.
     * @param <Value_> generic type of the values in the list variable.
     * @return unmodifiable, with fast random access; empty if no entity accepts the value.
     */
    <Entity_, Value_> List<Value_> getValuesSharingRangeWith(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value);

}
