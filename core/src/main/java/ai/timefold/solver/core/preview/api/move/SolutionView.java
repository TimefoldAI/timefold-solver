package ai.timefold.solver.core.preview.api.move;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.entity.PlanningPinToIndex;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Allows read-only access to the state of the solution that is being operated on by the {@link Move}.
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
public interface SolutionView<Solution_> {

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
     * @throws NullPointerException if the value of the list variable is null
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    <Entity_, Value_> int countValues(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity);

    /**
     * Reads the value of a {@link PlanningListVariable list planning variable} of a given entity at a specific index.
     *
     * @param variableMetaModel Describes the variable whose value is to be read.
     * @param entity The entity whose variable is to be read.
     * @param index >= 0
     * @return The value at the given index in the list variable.
     * @throws NullPointerException if the value of the list variable is null
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    <Entity_, Value_> Value_ getValueAtIndex(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, int index);

    /**
     * Locates a given value in any {@link PlanningListVariable list planning variable}.
     *
     * @param variableMetaModel Describes the variable whose value is to be read.
     * @param value The value to locate.
     * @return the location of the value in the variable
     */
    <Entity_, Value_> ElementPosition getPositionOf(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value);

    /**
     * Checks if a {@link PlanningEntity} with a basic {@link PlanningVariable} is pinned.
     *
     * @param variableMetaModel Describes the variable whose value is to be read.
     * @param entity The entity to check if it is pinned.
     * @return boolean indicating if the value is pinned in the variable
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
     * @param value The value to check if it is pinned; may be null, in which case the method returns false.
     * @return boolean indicating if the value is pinned in the variable
     */
    <Entity_, Value_> boolean isPinned(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @Nullable Value_ value);

    /**
     * Checks if a given value is present in the value range of a genuine planning variable,
     * when the value range is defined on {@link PlanningSolution}.
     *
     * @param variableMetaModel variable in question
     * @param value value to check
     * @return true if the value is acceptable for the variable
     * @param <Entity_> generic type of the entity that the variable is defined on
     * @param <Value_> generic type of the value that the variable can take
     * @throws IllegalArgumentException if the value range is on an entity as opposed to a solution;
     *         use {@link #isValueInRange(GenuineVariableMetaModel, Object, Object)} to provide the entity instance.
     */
    default <Entity_, Value_> boolean isValueInRange(GenuineVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @Nullable Value_ value) {
        return isValueInRange(variableMetaModel, null, value);
    }

    /**
     * Checks if a given value is present in the value range of a genuine planning variable.
     * If the value range is defined on {@link PlanningEntity entity},
     * the {@code entity} argument must not be null.
     *
     * @param variableMetaModel variable in question
     * @param entity entity that the value would be applied to;
     *        must be of a type that the variable is defined on
     * @param value value to check
     * @return true if the value is acceptable for the variable
     * @param <Entity_> generic type of the entity that the variable is defined on
     * @param <Value_> generic type of the value that the variable can take
     * @throws IllegalArgumentException if the value range is on an entity as opposed to a solution, and the entity is null
     */
    <Entity_, Value_> boolean isValueInRange(GenuineVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @Nullable Entity_ entity, @Nullable Value_ value);

}
