package ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart;

import java.time.Clock;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * Restart strategy, which exponentially increases the move count values that trigger the restart process.
 */
public class UnimprovedMoveCountRestartStrategy<Solution_> extends AbstractGeometricRestartStrategy<Solution_> {

    // Last checkpoint of a solution improvement or the restart process
    protected long lastCheckpoint;
    private Score<?> currentBestScore;

    public UnimprovedMoveCountRestartStrategy() {
        this(Clock.systemUTC());
    }

    protected UnimprovedMoveCountRestartStrategy(Clock clock) {
        // 50k moves as the multiplier
        super(clock, 50_000);
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
    public boolean process(LocalSearchMoveScope<Solution_> moveScope) {
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
