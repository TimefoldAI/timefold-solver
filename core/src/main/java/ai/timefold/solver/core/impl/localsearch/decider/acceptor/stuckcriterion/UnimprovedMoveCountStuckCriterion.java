package ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class UnimprovedMoveCountStuckCriterion<Solution_> implements StuckCriterion<Solution_> {

    private int countRejected;
    private Score<?> initialBestScore;
    private Score<?> lastCompletedScore;
    private int maxRejected;
    private boolean waitForFirstBestScore;

    public UnimprovedMoveCountStuckCriterion() {
    }

    public void setMaxRejected(int maxRejected) {
        this.maxRejected = maxRejected;
    }

    @Override
    public boolean isSolverStuck(LocalSearchMoveScope<Solution_> moveScope) {
        if (waitForFirstBestScore) {
            return false;
        }
        if (moveScope.getScore().compareTo(lastCompletedScore) > 0) {
            countRejected = 0;
        }
        countRejected++;
        return countRejected > maxRejected;
    }

    @Override
    public boolean isSolverStuck(LocalSearchStepScope<Solution_> stepScope) {
        // Only evaluated per move
        return false;
    }

    @Override
    public void reset(LocalSearchPhaseScope<Solution_> phaseScope) {
        this.countRejected = 0;
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        waitForFirstBestScore = true;
        countRejected = 0;
        initialBestScore = phaseScope.getBestScore();
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        lastCompletedScore = stepScope.getPhaseScope().getLastCompletedStepScope().getScore();
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        // Do nothing
        if (waitForFirstBestScore && ((Score) stepScope.getScore()).compareTo(initialBestScore) > 0) {
            waitForFirstBestScore = false;
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
