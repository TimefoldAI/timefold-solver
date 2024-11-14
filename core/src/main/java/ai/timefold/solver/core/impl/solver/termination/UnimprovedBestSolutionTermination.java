package ai.timefold.solver.core.impl.solver.termination;

import java.time.Clock;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

public final class UnimprovedBestSolutionTermination<Solution_> extends AbstractTermination<Solution_> {

    // Minimal interval of time to avoid early conclusions
    protected static final long MINIMAL_INTERVAL_TIME_MILLIS = 10_000L;
    // This setting determines the amount of time
    // that is allowed without any improvements since the last best solution was identified.
    // For example, if the last solution was found at 10 seconds and the setting is configured to 0.5,
    // the solver will stop if no improvement is made within 5 seconds.
    private final double flatLineDetectionRatio;
    // This criterion functions similarly to the flatLineDetectionRatio,
    // as it is also used to identify periods without improvement.
    // However, the key difference is that it focuses on detecting "flat lines" between solution improvements.
    // When a flat line is detected after the solution has improved,
    // it indicates that the previous duration was not enough to terminate the process.
    // However, it also indicates that the solver will begin
    // re-evaluating the termination criterion from the last improvement before the recent improvement.
    private final double newCurveDetectionRatio;
    private final Clock clock;
    // The field stores the time of the first best solution of the current curve.
    // If a solving process involves multiple curves,
    // the value is tied to the growth of the last curve analyzed.
    protected long initialCurvePointMillis;
    protected long lastImprovementMillis;
    private Score<?> previousBest;
    protected Score<?> currentBest;
    protected boolean waitForFirstBestScore;
    protected Boolean terminate;

    public UnimprovedBestSolutionTermination(Double flatLineDetectionRatio, Double newCurveDetectionRatio) {
        this(flatLineDetectionRatio, newCurveDetectionRatio, Clock.systemUTC());
    }

    public UnimprovedBestSolutionTermination(Double flatLineDetectionRatio, Double newCurveDetectionRatio, Clock clock) {
        this.flatLineDetectionRatio = Objects.requireNonNull(flatLineDetectionRatio,
                "The field flatLineDetectionRatio is required for the termination UnimprovedBestSolutionTermination");
        this.newCurveDetectionRatio = Objects.requireNonNull(newCurveDetectionRatio,
                "The field newCurveDetectionRatio is required for the termination UnimprovedBestSolutionTermination");
        this.clock = Objects.requireNonNull(clock);
        if (flatLineDetectionRatio < 0) {
            throw new IllegalArgumentException(
                    "The flatLineDetectionRatio (%.2f) cannot be negative.".formatted(flatLineDetectionRatio));
        }
        if (newCurveDetectionRatio < 0) {
            throw new IllegalArgumentException(
                    "The newCurveDetectionRatio (%.2f) cannot be negative.".formatted(newCurveDetectionRatio));
        }
        if (newCurveDetectionRatio > flatLineDetectionRatio) {
            throw new IllegalArgumentException(
                    "The newCurveDetectionRatio (%.2f) cannot be greater than flatLineDetectionRatio (%.2f)."
                            .formatted(newCurveDetectionRatio, flatLineDetectionRatio));
        }
    }

    public double getFlatLineDetectionRatio() {
        return flatLineDetectionRatio;
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    @SuppressWarnings("unchecked")
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        initialCurvePointMillis = clock.millis();
        lastImprovementMillis = 0L;
        currentBest = phaseScope.getBestScore();
        previousBest = currentBest;
        waitForFirstBestScore = true;
        terminate = null;
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        terminate = null;
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
        if (terminate != null) {
            return terminate;
        }
        // Validate if there is a first best solution and the poll time
        if (waitForFirstBestScore) {
            return false;
        }
        var currentTimeMillis = clock.millis();
        var improved = currentBest.compareTo(phaseScope.getBestScore()) < 0;
        var completeInterval = currentTimeMillis - initialCurvePointMillis;
        var newInterval = currentTimeMillis - lastImprovementMillis;
        if (improved) {
            // If there is a flat line between the last and new best solutions,
            // the initial value becomes the most recent best score,
            // as it would be the starting point for the new curve.
            var minInterval = Math.floor(completeInterval * newCurveDetectionRatio);
            var maxInterval = Math.floor(completeInterval * flatLineDetectionRatio);
            if (lastImprovementMillis > 0 && completeInterval >= MINIMAL_INTERVAL_TIME_MILLIS && newInterval > minInterval
                    && newInterval < maxInterval) {
                initialCurvePointMillis = lastImprovementMillis;
                previousBest = currentBest;
                if (logger.isInfoEnabled()) {
                    logger.debug("Starting a new curve with ({}), estimated time interval ({}s)",
                            previousBest,
                            String.format("%.2f", completeInterval / 1000.0));
                }
            }
            lastImprovementMillis = currentTimeMillis;
            currentBest = phaseScope.getBestScore();
            terminate = null;
            return false;
        } else {
            if (completeInterval < MINIMAL_INTERVAL_TIME_MILLIS) {
                return false;
            }
            var maxInterval = Math.floor(completeInterval * flatLineDetectionRatio);
            if (newInterval > maxInterval) {
                terminate = true;
                return true;
            } else {
                terminate = null;
                return false;
            }
        }
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

    @Override
    public UnimprovedBestSolutionTermination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        return new UnimprovedBestSolutionTermination<>(flatLineDetectionRatio, newCurveDetectionRatio, clock);
    }

    @Override
    public String toString() {
        return "UnimprovedBestSolutionTermination(%.2f, %.2f)".formatted(flatLineDetectionRatio, newCurveDetectionRatio);
    }
}
