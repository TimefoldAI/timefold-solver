package ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart;

import java.time.Clock;
import java.time.Instant;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.reconfiguration.RestartStrategy;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * Criterion based on the unimproved move count.
 * It exponentially increases the unimproved move count values that trigger the
 * {@link RestartStrategy restart} process.
 */
public class UnimprovedMoveCountStuckCriterion<Solution_> extends AbstractGeometricStuckCriterion<Solution_> {

    // 50k moves multiplier defined through experiments
    protected static final long UNIMPROVED_MOVE_COUNT_MULTIPLIER = 50_000;
    // Last checkpoint of a solution improvement or the restart process
    protected long lastCheckpoint;
    private Score<?> currentBestScore;

    public UnimprovedMoveCountStuckCriterion() {
        this(Instant.now(Clock.systemUTC()));
    }

    protected UnimprovedMoveCountStuckCriterion(Instant instant) {
        super(instant, UNIMPROVED_MOVE_COUNT_MULTIPLIER);
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        currentBestScore = phaseScope.getBestScore();
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
}
