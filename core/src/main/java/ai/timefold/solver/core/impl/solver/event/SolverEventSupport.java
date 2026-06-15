package ai.timefold.solver.core.impl.solver.event;

import java.util.concurrent.locks.Lock;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.event.EventProducerId;
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

    public void fireBestSolutionChanged(SolverScope<Solution_> solverScope, EventProducerId eventProducerId,
            Solution_ newBestSolution, Lock readLock) {
        var it = getEventListeners().iterator();
        var timeMillisSpent = solverScope.getBestSolutionTimeMillisSpent();
        var bestScore = solverScope.getBestScore();
        if (it.hasNext()) {
            var event =
                    new DefaultBestSolutionChangedEvent<>(solver, eventProducerId, timeMillisSpent, newBestSolution, bestScore);
            do {
                var eventListener = it.next();
                if (eventListener instanceof LockableSolverEventListener<Solution_> lockableSolverEventListener) {
                    lockableSolverEventListener.bestSolutionChanged(event, readLock);
                } else {
                    eventListener.bestSolutionChanged(event);
                }
            } while (it.hasNext());
        }
    }

}
