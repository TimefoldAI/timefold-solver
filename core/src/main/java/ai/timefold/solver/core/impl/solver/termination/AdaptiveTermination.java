package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NonNull;

public final class AdaptiveTermination<Solution_, Score_ extends Score<Score_>> implements Termination<Solution_> {
    static final long NANOS_PER_MILLISECOND = 1_000_000;

    private final long gracePeriodNanos;
    private final double minimumImprovementRatio;

    private boolean isGracePeriodActive;
    private long gracePeriodStartTimeNanos;
    private double gracePeriodSoftestImprovementDouble;

    private final AdaptiveScoreRingBuffer<Score_> scoresByTime;

    public AdaptiveTermination(long gracePeriodMillis, double minimumImprovementRatio) {
        // convert to nanoseconds here so we don't need to do a
        // division in the hot loop
        this.gracePeriodNanos = gracePeriodMillis * NANOS_PER_MILLISECOND;
        this.minimumImprovementRatio = minimumImprovementRatio;
        this.scoresByTime = new AdaptiveScoreRingBuffer<>();
    }

    public long getGracePeriodNanos() {
        return gracePeriodNanos;
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
    private static <Score_ extends Score<Score_>> double softImprovementOrNaNForHarderChange(@NonNull Score_ start,
            @NonNull Score_ end) {
        if (start.equals(end)) {
            // optimization: since most of the time the score the same,
            // we can use equals to avoid creating double arrays in the
            // vast majority of comparisons
            return 0.0;
        }
        var scoreDiffs = end.subtract(start).toLevelDoubles();
        var softestLevel = scoreDiffs.length - 1;
        for (int i = 0; i < softestLevel; i++) {
            if (scoreDiffs[i] != 0.0) {
                return Double.NaN;
            }
        }
        return scoreDiffs[softestLevel];
    }

    public void start(long startTime, Score_ startingScore) {
        resetGracePeriod(startTime, startingScore);
    }

    public void step(long time, Score_ bestScore) {
        scoresByTime.put(time, bestScore);
    }

    private void resetGracePeriod(long currentTime, Score_ startingScore) {
        gracePeriodStartTimeNanos = currentTime;
        isGracePeriodActive = true;

        // Remove all entries in the map since grace is reset
        scoresByTime.clear();

        // Put the current best score as the first entry
        scoresByTime.put(currentTime, startingScore);
    }

    public boolean isTerminated(long currentTime, Score_ endScore) {
        if (isGracePeriodActive) {
            // first score in scoresByTime = first score in grace period window
            var endpointDiff = softImprovementOrNaNForHarderChange(scoresByTime.peekFirst(), endScore);
            if (Double.isNaN(endpointDiff)) {
                resetGracePeriod(currentTime, endScore);
                return false;
            }
            var timeElapsedNanos = currentTime - gracePeriodStartTimeNanos;
            if (timeElapsedNanos >= gracePeriodNanos) {
                // grace period over, record the reference diff
                isGracePeriodActive = false;
                gracePeriodSoftestImprovementDouble = endpointDiff;
                return endpointDiff == 0.0;
            }
            return false;
        }

        var startScore = scoresByTime.pollLatestScoreBeforeTimeAndClearPrior(currentTime - gracePeriodNanos);
        var scoreDiff = softImprovementOrNaNForHarderChange(startScore, endScore);
        if (Double.isNaN(scoreDiff)) {
            resetGracePeriod(currentTime, endScore);
            return false;
        }

        return scoreDiff / gracePeriodSoftestImprovementDouble < minimumImprovementRatio;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        return isTerminated(System.nanoTime(), (Score_) solverScope.getBestScore());
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        return isTerminated(System.nanoTime(), phaseScope.getBestScore());
    }

    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        return -1.0;
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        return -1.0;
    }

    @Override
    public Termination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        return new AdaptiveTermination<>(gracePeriodNanos, minimumImprovementRatio);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        // intentionally empty - no work to do
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        // intentionally empty - no work to do
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        step(System.nanoTime(), stepScope.getPhaseScope().getBestScore());
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        // intentionally empty - no work to do
    }

    @Override
    @SuppressWarnings("unchecked")
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        start(System.nanoTime(), (Score_) solverScope.getBestScore());
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        scoresByTime.clear();
    }
}
