package ai.timefold.solver.core.api.solver.event;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.SolverRunSummary;

/**
 * Delivered in a consumer thread at the end of the solving process and contains the final {@link PlanningSolution best
 * solution} found, together with the statistics of the run.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public interface FinalBestSolutionEvent<Solution_> extends SolverRunSummary {
    /**
     * @return the {@link PlanningSolution best solution} found by the solver
     */
    Solution_ solution();
}
