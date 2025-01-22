package ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart;

import java.time.Clock;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restart strategy which exponentially increases the restart times.
 * The first restart occurs after one second without improvement.
 * Following that, the unimproved timeout increases exponentially: 1s, 1s, 2s, 3s, 4s, 5s, 8s...
 * <p>
 * The strategy is based on the work: Search in a Small World by Toby Walsh
 */
public class UnimprovedTimeGeometricRestartStrategy<Solution_> implements RestartStrategy<Solution_> {
    private static final double GEOMETRIC_FACTOR = 1.4; // Value extracted from the cited paper
    private static final double SCALING_FACTOR = 1.0;

    private final Logger logger = LoggerFactory.getLogger(UnimprovedTimeGeometricRestartStrategy.class);
    private final Clock clock;

    protected long lastImprovementMillis;
    private Score<?> currentBestScore;
    protected boolean restartTriggered;
    private double geometricGrowFactor;
    protected long nextRestart;

    public UnimprovedTimeGeometricRestartStrategy() {
        this(Clock.systemUTC());
    }

    protected UnimprovedTimeGeometricRestartStrategy(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        currentBestScore = phaseScope.getBestScore();
        geometricGrowFactor = 1;
        nextRestart = (long) (1_000 * SCALING_FACTOR);
        restartTriggered = false;
        lastImprovementMillis = clock.millis();
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        // Do nothing
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        this.currentBestScore = stepScope.getPhaseScope().getBestScore();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        if (((Score) stepScope.getScore()).compareTo(currentBestScore) > 0) {
            lastImprovementMillis = clock.millis();
            this.currentBestScore = stepScope.getScore();
        }
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public boolean isTriggered(LocalSearchMoveScope<Solution_> moveScope) {
        if (!restartTriggered && lastImprovementMillis > 0
                && clock.millis() - lastImprovementMillis >= nextRestart) {
            logger.debug("Restart triggered with geometric factor {} and scaling factor of {}", geometricGrowFactor,
                    SCALING_FACTOR);
            nextRestart = (long) Math.ceil(SCALING_FACTOR * geometricGrowFactor * 1_000);
            geometricGrowFactor = Math.ceil(geometricGrowFactor * GEOMETRIC_FACTOR);
            lastImprovementMillis = clock.millis();
            restartTriggered = true;
        }
        return restartTriggered;
    }

    @Override
    public void reset() {
        restartTriggered = false;
        lastImprovementMillis = clock.millis();
    }
}
