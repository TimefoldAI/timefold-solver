package ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart;

import ai.timefold.solver.core.impl.localsearch.event.LocalSearchPhaseLifecycleListener;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;

/**
 * Base contract
 * for defining strategies that identify when the {@link ai.timefold.solver.core.api.solver.Solver solver} is stuck.
 * 
 * @param <Solution_>
 */
public interface StuckCriterion<Solution_> extends LocalSearchPhaseLifecycleListener<Solution_> {

    /**
     * Main logic that applies a specific metric to determine if a solver is stuck and cannot find better solutions
     * in the current solving flow.
     * 
     * @param moveScope cannot be null
     * @return
     */
    boolean isSolverStuck(LocalSearchMoveScope<Solution_> moveScope);
}
