package ai.timefold.solver.core.preview.api.move;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Allows read-only access to the state of the solution that is being operated on by the {@link Move}.
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
public interface SolutionView<Solution_> {

    /**
     * Reads the value of a @{@link PlanningVariable basic planning variable} of a given entity.
     * 
     * @param variableMetaModel Describes the variable whose value is to be read.
     * @param entity The entity whose variable is to be read.
     * @return The value of the variable on the entity.
     */
    <Entity_, Value_> @Nullable Value_
            getValue(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity);

    /**
     * Reads the value of a @{@link PlanningListVariable list planning variable} of a given entity at a specific index.
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
     * Locates a given value in any @{@link PlanningListVariable list planning variable}.
     *
     * @param variableMetaModel Describes the variable whose value is to be read.
     * @param value The value to locate.
     * @return the location of the value in the variable
     */
    <Entity_, Value_> ElementPosition getPositionOf(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value);

}
