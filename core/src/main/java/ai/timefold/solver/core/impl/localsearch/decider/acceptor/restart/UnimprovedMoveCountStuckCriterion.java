package ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart;

import java.time.Clock;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.reconfiguration.RestartStrategy;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * Criterion based on the unimproved move count.
 * It exponentially increases the unimproved move count values that trigger the
 * {@link RestartStrategy restart} process.
 */
public class UnimprovedMoveCountStuckCriterion<Solution_> extends AbstractGeometricStuckCriterion<Solution_> {

    // The multiplier will lead to an unimproved move count near a 10-second window without any improvements.
    // In this manner, the first restart will occur after approximately 10 seconds without any improvement,
    // then after 20 seconds, and so on.
    protected static final double UNIMPROVED_MOVE_COUNT_MULTIPLIER = 10.0;
    // Last checkpoint of a solution improvement or the restart process
    protected long lastCheckpoint;
    private Score<?> currentBestScore;

    public UnimprovedMoveCountStuckCriterion() {
        this(Clock.systemUTC());
    }

    protected UnimprovedMoveCountStuckCriterion(Clock clock) {
        super(clock);
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        this.currentBestScore = stepScope.getPhaseScope().getBestScore();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        // Do not update it during the grace period
        if (isGracePeriodFinished() && ((Score) stepScope.getScore()).compareTo(currentBestScore) > 0) {
            lastCheckpoint = stepScope.getPhaseScope().getSolverScope().getMoveEvaluationCount();
        }
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        lastCheckpoint = 0;
    }

    @Override
    public boolean evaluateCriterion(LocalSearchMoveScope<Solution_> moveScope) {
        var currentMoveCount = moveScope.getStepScope().getPhaseScope().getSolverScope().getMoveEvaluationCount();
        if (lastCheckpoint == 0) {
            lastCheckpoint = currentMoveCount;
            return false;
        }
        if (currentMoveCount - lastCheckpoint >= nextRestart) {
            lastCheckpoint = currentMoveCount;
            return true;
        }
        return false;
    }

    @Override
    double calculateScalingFactor(LocalSearchMoveScope<Solution_> moveScope) {
        return moveScope.getStepScope().getPhaseScope().getSolverScope().getMoveEvaluationSpeed()
                * UNIMPROVED_MOVE_COUNT_MULTIPLIER;
    }
}
