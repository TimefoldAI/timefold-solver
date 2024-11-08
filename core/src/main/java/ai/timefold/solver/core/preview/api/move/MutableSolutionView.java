package ai.timefold.solver.core.preview.api.move;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Contains all reading and mutating methods available to a {@link Move}
 * in order to change the state of a {@link PlanningSolution planning solution}.
 * <p>
 * <strong>This package and all of its contents are part of the Move Streams API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it,
 * before we finalize the API.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver Github</a>.
 * 
 * @param <Solution_>
 */
public interface MutableSolutionView<Solution_> extends SolutionView<Solution_> {

    /**
     * Reads the value of a @{@link PlanningVariable basic planning variable} of a given entity.
     * 
     * @param variableMetaModel Describes the variable to be changed.
     * @param entity The entity whose variable value is to be changed.
     * @param newValue maybe null, if unassigning the variable
     */
    <Entity_, Value_> void changeVariable(@NonNull PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @NonNull Entity_ entity, @Nullable Value_ newValue);

    /**
     * Moves a value from one entity's {@link PlanningListVariable planning list variable} to another.
     *
     * @param variableMetaModel Describes the variable to be changed.
     * @param sourceEntity The first entity whose variable value is to be changed.
     * @param sourceIndex >= 0
     * @param destinationEntity The second entity whose variable value is to be changed.
     * @param destinationIndex >= 0
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    <Entity_, Value_> void moveValueBetweenLists(
            @NonNull PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @NonNull Entity_ sourceEntity, int sourceIndex, @NonNull Entity_ destinationEntity, int destinationIndex);

    /**
     * Moves a value within one entity's {@link PlanningListVariable planning list variable}.
     *
     * @param variableMetaModel Describes the variable to be changed.
     * @param entity The entity whose variable value is to be changed.
     * @param sourceIndex >= 0
     * @param destinationIndex >= 0
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    <Entity_, Value_> void moveValueInList(@NonNull PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @NonNull Entity_ entity, int sourceIndex, int destinationIndex);

    /**
     * Tells the underlying {@link ScoreDirector}
     * to notify the solver of the mutating operations performed by the {@link Move}.
     */
    void updateShadowVariables();

}
