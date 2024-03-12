package ai.timefold.solver.core.impl.solver.event;

import java.util.Iterator;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent;
import ai.timefold.solver.core.api.solver.event.SolverEventListener;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * Internal API.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SolverEventSupport<Solution_> extends AbstractEventSupport<SolverEventListener<Solution_>> {

    private final Solver<Solution_> solver;

    public SolverEventSupport(Solver<Solution_> solver) {
        this.solver = solver;
    }

    public void fireBestSolutionChanged(SolverScope<Solution_> solverScope, Solution_ newBestSolution) {
        final Iterator<SolverEventListener<Solution_>> it = getEventListeners().iterator();
        long timeMillisSpent = solverScope.getBestSolutionTimeMillisSpent();
        Score bestScore = solverScope.getBestScore();
        if (it.hasNext()) {
            final BestSolutionChangedEvent<Solution_> event = new BestSolutionChangedEvent<>(solver,
                    timeMillisSpent, newBestSolution, bestScore);
            do {
                it.next().bestSolutionChanged(event);
            } while (it.hasNext());
        }
    }

}
