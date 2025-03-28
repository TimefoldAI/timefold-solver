package ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.impl.localsearch.event.LocalSearchPhaseLifecycleListener;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

/**
 * Allow defining strategies that identify when the {@link Solver solver} is stuck.
 * 
 * @param <Solution_> the solution type
 */
public interface StuckCriterion<Solution_> extends LocalSearchPhaseLifecycleListener<Solution_> {

    /**
     * Main logic that applies a specific metric to determine if a solver is stuck in a local optimum.
     * 
     * @param stepScope cannot be null
     */
    boolean isSolverStuck(LocalSearchStepScope<Solution_> stepScope);

    /**
     * Same as {{@link #isSolverStuck(LocalSearchStepScope)}, but it is called for every move}
     * 
     * @param moveScope cannot be null
     * @return
     */
    boolean isSolverStuck(LocalSearchMoveScope<Solution_> moveScope);

    void reset(LocalSearchPhaseScope<Solution_> phaseScope);
}
