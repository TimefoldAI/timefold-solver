package ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion;

import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restart strategy, which exponentially increases the metric that triggers the restart process.
 * The first restart occurs after the {@code scalingFactor * GEOMETRIC_FACTOR^restartCount} metric.
 * Following that, the metric increases exponentially: 1, 2, 3, 5, 7...
 * <p>
 * The strategy is based on the work: Search in a Small World by Toby Walsh
 * 
 * @param <Solution_> the solution type
 */
public abstract class AbstractGeometricStuckCriterion<Solution_> implements StuckCriterion<Solution_> {
    protected static final Logger logger = LoggerFactory.getLogger(AbstractGeometricStuckCriterion.class);
    private static final double GEOMETRIC_FACTOR = 1.4; // Value extracted from the cited paper

    private double scalingFactor;
    protected long nextRestart;
    private double currentGeometricGrowFactor;

    protected AbstractGeometricStuckCriterion(double scalingFactor) {
        this.scalingFactor = scalingFactor;
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        // Do nothing
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        // Do nothing
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        currentGeometricGrowFactor = 1;
        nextRestart = calculateNextRestart();
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    public double getScalingFactor() {
        return scalingFactor;
    }

    public void setScalingFactor(double scalingFactor) {
        this.scalingFactor = scalingFactor;
        currentGeometricGrowFactor = 1;
        nextRestart = calculateNextRestart();
    }

    @Override
    public boolean isSolverStuck(LocalSearchStepScope<Solution_> stepScope) {
        var triggered = evaluateCriterion(stepScope);
        if (triggered) {
            logger.info(
                    "Restart triggered with geometric factor ({}), scaling factor of ({}), best score ({})",
                    currentGeometricGrowFactor, scalingFactor,
                    stepScope.getPhaseScope().getBestScore());
            currentGeometricGrowFactor = Math.ceil(currentGeometricGrowFactor * GEOMETRIC_FACTOR);
            nextRestart = calculateNextRestart();
            return true;
        }
        return false;
    }

    private long calculateNextRestart() {
        return (long) Math.ceil(currentGeometricGrowFactor * scalingFactor);
    }

    abstract boolean evaluateCriterion(LocalSearchStepScope<Solution_> stepScope);

}
