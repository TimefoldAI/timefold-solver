package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.phase.custom.scope.CustomPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class DiminishedReturnsTermination<Solution_, Score_ extends Score<Score_>>
        extends AbstractPhaseTermination<Solution_>
        implements ChildThreadSupportingTermination<Solution_, SolverScope<Solution_>> {

    static final long NANOS_PER_MILLISECOND = 1_000_000;

    private final long slidingWindowNanos;
    private final double minimumImprovementRatio;

    private boolean isGracePeriodActive;
    private long gracePeriodStartTimeNanos;
    private double gracePeriodSoftestImprovementDouble;

    private final DiminishedReturnsScoreRingBuffer<Score_> scoresByTime;

    public DiminishedReturnsTermination(long slidingWindowMillis, double minimumImprovementRatio) {
        if (slidingWindowMillis < 0L) {
            throw new IllegalArgumentException("The slidingWindowMillis (%d) cannot be negative."
                    .formatted(slidingWindowMillis));
        }

        if (minimumImprovementRatio <= 0.0) {
            throw new IllegalArgumentException("The minimumImprovementRatio (%f) must be positive."
                    .formatted(minimumImprovementRatio));
        }

        // convert to nanoseconds here so we don't need to do a
        // division in the hot loop
        this.slidingWindowNanos = slidingWindowMillis * NANOS_PER_MILLISECOND;
        this.minimumImprovementRatio = minimumImprovementRatio;
        this.scoresByTime = new DiminishedReturnsScoreRingBuffer<>();
    }

    public long getSlidingWindowNanos() {
        return slidingWindowNanos;
    }

    public double getMinimumImprovementRatio() {
        return minimumImprovementRatio;
    }

    /**
     * Returns the improvement in the softest level between the prior
     * and current best scores as a double, or {@link Double#NaN} if
     * there is a difference in any of their other levels.
     *
     * @param start the prior best score
     * @param end the current best score
     * @return the softest level difference between end and start, or
     *         {@link Double#NaN} if a harder level changed
     * @param <Score_> The score type
     */
    private static <Score_ extends Score<Score_>> double softImprovementOrNaNForHarderChange(InnerScore<Score_> start,
            InnerScore<Score_> end) {
        if (start.equals(end)) {
            // optimization: since most of the time the score the same,
            // we can use equals to avoid creating double arrays in the
            // vast majority of comparisons
            return 0.0;
        }
        if (start.unassignedCount() != end.unassignedCount()) {
            // init score improved
            return Double.NaN;
        }
        var scoreDiffs = end.raw().subtract(start.raw()).toLevelDoubles();
        var softestLevel = scoreDiffs.length - 1;
        for (int i = 0; i < softestLevel; i++) {
            if (scoreDiffs[i] != 0.0) {
                return Double.NaN;
            }
        }
        return scoreDiffs[softestLevel];
    }

    public void start(long startTime, InnerScore<Score_> startingScore) {
        resetGracePeriod(startTime, startingScore);
    }

    public void step(long time, InnerScore<Score_> bestScore) {
        scoresByTime.put(time, bestScore);
    }

    private void resetGracePeriod(long currentTime, InnerScore<Score_> startingScore) {
        gracePeriodStartTimeNanos = currentTime;
        isGracePeriodActive = true;

        // Remove all entries in the map since grace is reset
        scoresByTime.clear();

        // Put the current best score as the first entry
        scoresByTime.put(currentTime, startingScore);
    }

    public boolean isTerminated(long currentTime, InnerScore<Score_> endScore) {
        if (isGracePeriodActive) {
            // first score in scoresByTime = first score in grace period window
            var endpointDiff = softImprovementOrNaNForHarderChange(scoresByTime.peekFirst(), endScore);
            if (Double.isNaN(endpointDiff)) {
                resetGracePeriod(currentTime, endScore);
                return false;
            }
            var timeElapsedNanos = currentTime - gracePeriodStartTimeNanos;
            if (timeElapsedNanos >= slidingWindowNanos) {
                // grace period over, record the reference diff
                isGracePeriodActive = false;
                gracePeriodSoftestImprovementDouble = endpointDiff;
                if (endpointDiff < 0.0) {
                    // Should be impossible; the only cases where the best score improves
                    // and have a lower softest level are if either a harder level or init score
                    // improves, but if that happens, we reset the grace period.
                    throw new IllegalStateException(
                            "Impossible state: The score deteriorated from (%s) to (%s) during the grace period."
                                    .formatted(scoresByTime.peekFirst(), endScore));
                }
                return endpointDiff == 0.0;
            }
            return false;
        }

        var startScore = scoresByTime.pollLatestScoreBeforeTimeAndClearPrior(currentTime - slidingWindowNanos);
        var scoreDiff = softImprovementOrNaNForHarderChange(startScore, endScore);
        if (Double.isNaN(scoreDiff)) {
            resetGracePeriod(currentTime, endScore);
            return false;
        }

        if (gracePeriodSoftestImprovementDouble == 0.0) {
            // The termination may be queried multiple times, even after completion.
            // We must ensure the grace period improvement is greater than zero to avoid division by zero errors.
            return true;
        }

        return scoreDiff / gracePeriodSoftestImprovementDouble < minimumImprovementRatio;
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        return isTerminated(System.nanoTime(), phaseScope.getBestScore());
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        return -1.0;
    }

    @Override
    public Termination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        return new DiminishedReturnsTermination<>(slidingWindowNanos, minimumImprovementRatio);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        start(System.nanoTime(), phaseScope.getBestScore());
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        scoresByTime.clear();
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        step(System.nanoTime(), stepScope.getPhaseScope().getBestScore());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean isApplicableTo(Class<? extends AbstractPhaseScope> phaseScopeClass) {
        return !(phaseScopeClass == ConstructionHeuristicPhaseScope.class
                || phaseScopeClass == CustomPhaseScope.class);
    }

    @Override
    public String toString() {
        return "DiminishedReturns()";
    }
}
