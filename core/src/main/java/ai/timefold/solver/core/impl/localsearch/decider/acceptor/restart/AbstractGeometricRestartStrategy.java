package ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart;

import java.time.Clock;

import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restart strategy, which exponentially increases the metric that triggers the restart process.
 * The first restart occurs after the {@code grace period + 1 * scalingFactor} metric.
 * Following that, the metric increases exponentially: 1, 2, 3, 5, 7, 10, 14...
 * <p>
 * The strategy is based on the work: Search in a Small World by Toby Walsh
 * 
 * @param <Solution_> the solution type
 */
public abstract class AbstractGeometricRestartStrategy<Solution_> implements RestartStrategy<Solution_> {
    private static final double GEOMETRIC_FACTOR = 1.4; // Value extracted from the cited paper
    protected final Clock clock;
    protected final Logger logger = LoggerFactory.getLogger(AbstractGeometricRestartStrategy.class);
    protected final double scalingFactor;

    private boolean gracePeriodFinished;
    private long gracePeriodMillis;
    protected long nextRestart;
    private double currentGeometricGrowFactor;

    protected AbstractGeometricRestartStrategy(Clock clock, double scalingFactor) {
        this.clock = clock;
        this.scalingFactor = scalingFactor;
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        if (gracePeriodMillis == 0) {
            // 10 seconds of grace period
            gracePeriodMillis = clock.millis() + 10_000;
        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        // Do nothing
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        currentGeometricGrowFactor = 1;
        gracePeriodMillis = 0;
        gracePeriodFinished = false;
        nextRestart = calculateNextRestart();
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public boolean isTriggered(LocalSearchMoveScope<Solution_> moveScope) {
        if (isGracePeriodFinished()) {
            var triggered = process(moveScope);
            if (triggered) {
                logger.trace("Restart triggered with geometric factor {}, scaling factor of {}", currentGeometricGrowFactor,
                        scalingFactor);
                currentGeometricGrowFactor = Math.ceil(currentGeometricGrowFactor * GEOMETRIC_FACTOR);
                nextRestart = calculateNextRestart();
                return true;
            }
        }
        return false;
    }

    protected boolean isGracePeriodFinished() {
        if (gracePeriodFinished) {
            return true;
        }
        gracePeriodFinished = clock.millis() >= gracePeriodMillis;
        return gracePeriodFinished;
    }

    private long calculateNextRestart() {
        return (long) Math.ceil(currentGeometricGrowFactor * scalingFactor);
    }

    abstract boolean process(LocalSearchMoveScope<Solution_> moveScope);

}
