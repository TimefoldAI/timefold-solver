package ai.timefold.solver.core.api.solver;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

import org.jspecify.annotations.NullMarked;

/**
 * The final best solution of a solver run, together with the statistics of that run.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public interface SolverRunResult<Solution_> extends SolverRunSummary {

    /**
     * @return the {@link PlanningSolution best solution} found by the solver
     */
    Solution_ solution();

}
