package ai.timefold.solver.core.api.move;

import ai.timefold.solver.core.api.domain.lookup.LookUpStrategyType;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.metamodel.BasicVariableMetaModel;
import ai.timefold.solver.core.api.domain.metamodel.ElementLocation;
import ai.timefold.solver.core.api.domain.metamodel.ListVariableMetaModel;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.change.ProblemChange;

/**
 * Allows read-only access to the state of the solution that is being operated on by the {@link Move}
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
 * @param <Solution_>
 */
public interface SolutionState<Solution_> {

    /**
     * Reads the value of a @{@link PlanningVariable basic planning variable} of a given entity.
     * 
     * @param variableMetaModel never null
     * @param entity never null
     * @return maybe null; the value of the variable on the entity
     * @param <Entity_>
     * @param <Value_>
     */
    <Entity_, Value_> Value_ getValue(BasicVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity);

    /**
     * Reads the value of a @{@link PlanningListVariable list planning variable} of a given entity at a specific index.
     * 
     * @param variableMetaModel never null
     * @param entity never null
     * @param index >= 0
     * @return maybe null; the value of the variable on the entity at the index
     * @throws NullPointerException if the value of the list variable is null
     * @throws IndexOutOfBoundsException if the index is out of bounds
     * @param <Entity_>
     * @param <Value_>
     */
    <Entity_, Value_> Value_ getValueAtIndex(ListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, int index);

    /**
     * Locates a given value in any @{@link PlanningListVariable list planning variable}.
     * 
     * @param variableMetaModel never null
     * @param value never null
     * @return never null; the location of the value in the variable
     * @param <Entity_>
     * @param <Value_>
     */
    <Entity_, Value_> ElementLocation getPositionOf(ListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value);

    /**
     * Translates an entity or fact instance (often from another {@link Thread})
     * to another {@link ScoreDirector}'s internal working instance.
     * Useful for move rebasing and in a {@link ProblemChange} and for multi-threaded solving.
     * <p>
     * Matching is determined by the {@link LookUpStrategyType} on {@link PlanningSolution}.
     * Matching uses a {@link PlanningId} by default.
     *
     * @param problemFactOrPlanningEntity never null
     * @return null if externalObject is null
     * @throws IllegalArgumentException if there is no working object for the fact or entity,
     *         if it cannot be looked up,
     *         or if its class is not supported.
     * @throws IllegalStateException if it cannot be looked up
     * @param <T>
     */
    <T> T rebase(T problemFactOrPlanningEntity);

}
