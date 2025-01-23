package ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart;

import java.time.Clock;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * Restart strategy which exponentially increases the restart times.
 */
public class UnimprovedTimeRestartStrategy<Solution_> extends AbstractGeometricRestartStrategy<Solution_> {

    protected long lastImprovementMillis;
    private Score<?> currentBestScore;

    public UnimprovedTimeRestartStrategy() {
        this(Clock.systemUTC());
    }

    protected UnimprovedTimeRestartStrategy(Clock clock) {
        // 1 second as the multiplier
        super(clock, 1_000);
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
            lastImprovementMillis = clock.millis();
        }
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        lastImprovementMillis = 0;
    }

    @Override
    public boolean process(LocalSearchMoveScope<Solution_> moveScope) {
        var currentTime = clock.millis();
        if (lastImprovementMillis == 0) {
            lastImprovementMillis = currentTime;
            return false;
        }
        if (currentTime - lastImprovementMillis >= nextRestart) {
            logger.debug("Restart triggered with geometric factor {} and scaling factor of {}", currentGeometricGrowFactor,
                    scalingFactor);
            lastImprovementMillis = clock.millis();
            return true;
        }
        return false;
    }

    @Override
    public void reset(LocalSearchMoveScope<Solution_> moveScope) {
        disableTriggerFlag();
        // Do not update it during the grace period
        if (isGracePeriodFinished()) {
            lastImprovementMillis = clock.millis();
        }
    }
}
