package ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.impl.localsearch.event.LocalSearchPhaseLifecycleListener;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;

/**
 * Allow defining strategies that identify when the {@link Solver solver} is stuck.
 * 
 * @param <Solution_> the solution type
 */
public interface StuckCriterion<Solution_> extends LocalSearchPhaseLifecycleListener<Solution_> {

    /**
     * Main logic that applies a specific metric to determine if a solver is stuck in a local optimum.
     * 
     * @param moveScope cannot be null
     * @return
     */
    boolean isSolverStuck(LocalSearchMoveScope<Solution_> moveScope);
}
