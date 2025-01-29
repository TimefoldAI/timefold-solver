package ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart;

import java.time.Clock;
import java.time.Instant;

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
public abstract class AbstractGeometricStuckCriterion<Solution_> implements StuckCriterion<Solution_> {
    protected static final Logger logger = LoggerFactory.getLogger(AbstractGeometricStuckCriterion.class);
    private static final double GEOMETRIC_FACTOR = 1.4; // Value extracted from the cited paper
    private static final long GRACE_PERIOD_MILLIS = 30_000; // 30s by default

    private final Clock clock;
    private double scalingFactor;
    private boolean gracePeriodFinished;
    private Instant gracePeriodEnd;
    protected long nextRestart;
    private double currentGeometricGrowFactor;

    protected AbstractGeometricStuckCriterion(Clock clock) {
        this.clock = clock;
        this.scalingFactor = -1;
    }

    protected void setScalingFactor(double scalingFactor) {
        this.scalingFactor = scalingFactor;
        this.nextRestart = calculateNextRestart();
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        if (gracePeriodEnd == null) {
            // 30 seconds of grace period
            gracePeriodEnd = clock.instant().plusMillis(GRACE_PERIOD_MILLIS);
        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        // Do nothing
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        currentGeometricGrowFactor = 1;
        gracePeriodEnd = null;
        gracePeriodFinished = false;
        nextRestart = calculateNextRestart();
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public boolean isSolverStuck(LocalSearchMoveScope<Solution_> moveScope) {
        if (isGracePeriodFinished()) {
            var triggered = evaluateCriterion(moveScope);
            if (triggered) {
                if (scalingFactor == -1) {
                    throw new IllegalStateException("The scaling factor is not defined for this criterion.");
                }
                logger.trace(
                        "Restart triggered with geometric factor {}, scaling factor of {}, best score ({}), move count ({})",
                        currentGeometricGrowFactor,
                        scalingFactor, moveScope.getStepScope().getPhaseScope().getBestScore(),
                        moveScope.getStepScope().getPhaseScope().getSolverScope().getMoveEvaluationCount());
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
        gracePeriodFinished = clock.millis() >= gracePeriodEnd.toEpochMilli();
        return gracePeriodFinished;
    }

    private long calculateNextRestart() {
        return (long) Math.ceil(currentGeometricGrowFactor * scalingFactor);
    }

    abstract boolean evaluateCriterion(LocalSearchMoveScope<Solution_> moveScope);

}
