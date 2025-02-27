package ai.timefold.solver.core.impl.localsearch.decider.acceptor;

import ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion.StuckCriterion;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
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
        stuckCriterion.stepEnded(stepScope);
    }

    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        if (stuckCriterion.isSolverStuck(moveScope)) {
            moveScope.getStepScope().getPhaseScope().setSolverStuck(true);
            restartTriggered = true;
            return true;
        }
        return accept(moveScope);
    }

    protected abstract boolean accept(LocalSearchMoveScope<Solution_> moveScope);

    public abstract void restart(LocalSearchStepScope<Solution_> stepScope);
}
