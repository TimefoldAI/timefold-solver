package ai.timefold.solver.core.impl.localsearch.decider.acceptor;

import ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion.StuckCriterion;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * Base class designed to analyze whether the solving process needs to be restarted.
 * Additionally, it also calls a restart logic as a result of restarting the solving process.
 */
public abstract class RestartableAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    private final StuckCriterion<Solution_> stuckCriterion;
    protected boolean restartTriggered;

    protected RestartableAcceptor(StuckCriterion<Solution_> stuckCriterion) {
        this.stuckCriterion = stuckCriterion;
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        stuckCriterion.solvingStarted(solverScope);
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        stuckCriterion.phaseStarted(phaseScope);
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        stuckCriterion.phaseEnded(phaseScope);
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        stuckCriterion.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        if (stuckCriterion.isSolverStuck(stepScope)) {
            if (rejectRestartEvent()) {
                // We need to reset the criterion,
                // or it will trigger the restart event in the next evaluation
                stuckCriterion.reset(stepScope);
                restartTriggered = false;
            } else {
                stepScope.getPhaseScope().setSolverStuck(true);
                restartTriggered = true;
            }
        }
        stuckCriterion.stepEnded(stepScope);
    }

    /**
     * The stuck criterion may trigger a restart event, but the acceptor might choose to delay it.
     * This method rechecks the restart event condition on the acceptor side.
     */
    public abstract boolean rejectRestartEvent();

    /**
     * Run the restart event logic on the acceptor side.
     */
    public abstract void restart(LocalSearchStepScope<Solution_> stepScope);
}
