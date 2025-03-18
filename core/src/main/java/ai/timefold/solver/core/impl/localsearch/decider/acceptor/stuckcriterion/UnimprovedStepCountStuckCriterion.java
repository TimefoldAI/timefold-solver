package ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class UnimprovedStepCountStuckCriterion<Solution_> implements StuckCriterion<Solution_> {

    private int countRejected;
    private Score<?> lastCompletedScore;
    private int maxRejected;

    public UnimprovedStepCountStuckCriterion() {
    }

    public void setMaxRejected(int maxRejected) {
        this.maxRejected = maxRejected;
    }

    @Override
    public boolean isSolverStuck(LocalSearchStepScope<Solution_> stepScope) {
        if (((Score) stepScope.getScore()).compareTo(lastCompletedScore) <= 0) {
            countRejected++;
        }
        return countRejected > maxRejected;
    }

    @Override
    public void reset(LocalSearchPhaseScope<Solution_> phaseScope) {
        this.countRejected = 0;
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        this.countRejected = 0;
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        lastCompletedScore = stepScope.getPhaseScope().getLastCompletedStepScope().getScore();
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        if (((Score) stepScope.getScore()).compareTo(lastCompletedScore) > 0) {
            reset(stepScope.getPhaseScope());
        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        // Do nothing
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Do nothing
    }
}
