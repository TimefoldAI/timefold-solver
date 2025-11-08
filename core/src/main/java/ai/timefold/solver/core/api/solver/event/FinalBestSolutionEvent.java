package ai.timefold.solver.core.api.solver.event;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

/**
 * Delivered in a consumer thread at the end of the solving process and contains the final {@link PlanningSolution best
 * solution} found.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public interface FinalBestSolutionEvent<Solution_> {
    /**
     * @return the {@link PlanningSolution best solution} found by the solver
     */
    Solution_ solution();
}
