package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

public final class UnimprovedBestSolutionTermination<Solution_> extends AbstractTermination<Solution_> {

    // Minimal interval of time to avoid early conclusions
    protected static final long MINIMAL_INTERVAL_TIME = 10L;
    // The ratio specifies the minimum criteria to determine a flat line between two move count values.
    // A value of 0.2 represents 20% of the execution time of the current growth curve.
    // For example, the first best solution is found at 0 seconds,
    // while the last best solution is found at 60 seconds.
    // Given the total time of 60 seconds,
    // we will identify a flat line between the last best solution and the discovered new best solution
    // if the time difference exceeds 12 seconds.
    private final double maxUnimprovedBestSolutionLimit;
    // Similar to unimprovedBestSolutionLimit,
    // this criterion is specifically used to identify flat lines among multiple curves before the termination.
    // The goal is to adjust the stop criterion based on the latest curve found when there are several.
    private final double minUnimprovedBestSolutionLimit;
    // The field stores the first best solution move count found in the curve growth chart.
    // If a solving process involves multiple curves,
    // the value is tied to the growth of the last curve analyzed.
    protected long initialImprovementMoveCount;
    protected long lastImprovementMoveCount;
    protected long lastMoveEvaluationSpeed;
    private Score<?> previousBest;
    protected Score<?> currentBest;
    protected boolean waitForFirstBestScore;
    protected Boolean cachedResult;

    public UnimprovedBestSolutionTermination(double unimprovedBestSolutionLimit) {
        this.maxUnimprovedBestSolutionLimit = unimprovedBestSolutionLimit;
        // 80% of the max unimproved limit
        this.minUnimprovedBestSolutionLimit = unimprovedBestSolutionLimit * 0.8;
        if (unimprovedBestSolutionLimit < 0) {
            throw new IllegalArgumentException(
                    "The unimprovedBestSolutionLimit (%.2f) cannot be negative.".formatted(unimprovedBestSolutionLimit));
        }
    }

    public double getUnimprovedBestSolutionLimit() {
        return maxUnimprovedBestSolutionLimit;
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    @SuppressWarnings("unchecked")
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        initialImprovementMoveCount = 0L;
        lastImprovementMoveCount = 0L;
        lastMoveEvaluationSpeed = 0L;
        currentBest = phaseScope.getBestScore();
        previousBest = currentBest;
        waitForFirstBestScore = true;
        cachedResult = null;
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        cachedResult = null;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        if (waitForFirstBestScore) {
            waitForFirstBestScore = ((Score) currentBest).compareTo(stepScope.getScore()) >= 0;
        }
    }

    // ************************************************************************
    // Terminated methods
    // ************************************************************************

    @Override
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        throw new UnsupportedOperationException(
                "%s can only be used for phase termination.".formatted(getClass().getSimpleName()));
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        if (cachedResult != null) {
            return cachedResult;
        }
        // Validate if there is a first best solution and the poll time
        if (waitForFirstBestScore) {
            return false;
        }
        var currentMoveCount = phaseScope.getSolverScope().getMoveEvaluationCount();
        var improved = currentBest.compareTo(phaseScope.getBestScore()) < 0;
        lastMoveEvaluationSpeed = phaseScope.getSolverScope().getMoveEvaluationSpeed();
        var interval = calculateInterval(initialImprovementMoveCount, currentMoveCount);
        if (improved) {
            // If there is a flat line between the last and new best solutions,
            // the initial value becomes the most recent best score,
            // as it would be the starting point for the new curve.
            var minInterval = Math.floor(interval * minUnimprovedBestSolutionLimit);
            var maxInterval = Math.floor(interval * maxUnimprovedBestSolutionLimit);
            var newInterval = calculateInterval(lastImprovementMoveCount, currentMoveCount);
            if (lastImprovementMoveCount > 0 && interval >= MINIMAL_INTERVAL_TIME && newInterval > minInterval
                    && newInterval < maxInterval) {
                initialImprovementMoveCount = lastImprovementMoveCount;
                previousBest = currentBest;
                if (logger.isInfoEnabled()) {
                    logger.info("Starting a new curve with ({}), estimated time interval ({}s)",
                            previousBest,
                            String.format("%.2f", calculateInterval(0, initialImprovementMoveCount)));
                }
            }
            lastImprovementMoveCount = currentMoveCount;
            currentBest = phaseScope.getBestScore();
            cachedResult = false;
        } else {
            if (interval < MINIMAL_INTERVAL_TIME) {
                return false;
            }
            var maxInterval = Math.floor(interval * maxUnimprovedBestSolutionLimit);
            var newInterval = calculateInterval(lastImprovementMoveCount, currentMoveCount);
            cachedResult = newInterval > maxInterval;
        }
        return cachedResult;
    }

    // ************************************************************************
    // Time gradient methods
    // ************************************************************************

    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        throw new UnsupportedOperationException(
                "%s can only be used for phase termination.".formatted(getClass().getSimpleName()));
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        // The value will change during the solving process.
        // Therefore, it is not possible to provide a number asymptotically incrementally
        return -1.0;
    }

    // ************************************************************************
    // Other methods
    // ************************************************************************

    private double calculateInterval(long startMoveCount, long endMoveCount) {
        return (double) (endMoveCount - startMoveCount) / lastMoveEvaluationSpeed;
    }

    @Override
    public UnimprovedBestSolutionTermination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        return new UnimprovedBestSolutionTermination<>(maxUnimprovedBestSolutionLimit);
    }

    @Override
    public String toString() {
        return "UnimprovedMoveCountRatio(%.2f)".formatted(maxUnimprovedBestSolutionLimit);
    }

}
