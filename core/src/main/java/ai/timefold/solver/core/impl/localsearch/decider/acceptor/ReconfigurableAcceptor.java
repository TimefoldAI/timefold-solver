package ai.timefold.solver.core.impl.localsearch.decider.acceptor;

import ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart.StuckCriterion;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * Base class designed to analyze whether the solving process needs to be restarted.
 * Additionally, it also calls a reconfiguration logic as a result of restarting the solving process.
 */
public abstract class ReconfigurableAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    private final StuckCriterion<Solution_> stuckCriterionDetection;

    protected ReconfigurableAcceptor(StuckCriterion<Solution_> stuckCriterionDetection) {
        this.stuckCriterionDetection = stuckCriterionDetection;
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        stuckCriterionDetection.solvingStarted(solverScope);
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        stuckCriterionDetection.phaseStarted(phaseScope);
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        stuckCriterionDetection.phaseEnded(phaseScope);
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        stuckCriterionDetection.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        stuckCriterionDetection.stepEnded(stepScope);
    }

    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        if (stuckCriterionDetection.isSolverStuck(moveScope)) {
            moveScope.getStepScope().getPhaseScope().triggerSolverStuck();
            return true;
        }
        return applyAcceptanceCriteria(moveScope);
    }

    protected abstract boolean applyAcceptanceCriteria(LocalSearchMoveScope<Solution_> moveScope);
}
