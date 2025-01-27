package ai.timefold.solver.core.impl.localsearch.decider.reconfiguration;

import ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart.StuckCriterion;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;

/**
 * Base contract for defining restart strategies.
 * The restart process is initiated
 * when the {@link ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider decider} identifies
 * that the solver is {@link StuckCriterion stuck}
 * and requires some logic to alter the current solving flow.
 *
 * @param <Solution_> the solution type
 */
public sealed interface RestartStrategy<Solution_> extends PhaseLifecycleListener<Solution_>
        permits RestoreBestSolutionRestartStrategy {

    /**
     * Restarts the solver to help it to get unstuck and discover new better solutions.
     *
     * @param stepScope cannot be null
     */
    void applyRestart(AbstractStepScope<Solution_> stepScope);

    /**
     * Evaluates whether the solver is stuck based on specific criteria
     * and determines if reconfiguration logic needs to be applied.
     * 
     * @param stepScope cannot be null
     * 
     * @return true if the solver needs to be reconfigured; or false otherwise
     */
    default boolean isSolverStuck(AbstractStepScope<Solution_> stepScope) {
        return stepScope.getPhaseScope().isSolverStuck();
    }

}
