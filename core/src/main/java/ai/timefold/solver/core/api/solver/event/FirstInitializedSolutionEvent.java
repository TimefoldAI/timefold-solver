package ai.timefold.solver.core.api.solver.event;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

/**
 * Delivered in a consumer thread at the beginning of the actual optimization process.
 * First initialized solution is the solution at the end of the last phase
 * that immediately precedes the first local search phase.
 * 
 * @param <Solution_>
 */
public interface FirstInitializedSolutionEvent<Solution_> {
    /**
     * @return The {@link PlanningSolution initialized solution}
     */
    Solution_ solution();

    /**
     * @return A {@link EventProducerId} identifying what generated the event
     */
    EventProducerId producerId();

    /**
     * @return True if the solver was terminated early, false otherwise
     */
    boolean isTerminatedEarly();
}
