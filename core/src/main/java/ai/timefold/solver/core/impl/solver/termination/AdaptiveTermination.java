package ai.timefold.solver.core.impl.solver.termination;

import java.util.NavigableMap;
import java.util.TreeMap;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class AdaptiveTermination<Solution_, Score_ extends Score<Score_>> implements Termination<Solution_> {
    private final long gracePeriodMillis;
    private final double minimumImprovementRatio;

    boolean isGracePeriodActive;
    private long gracePeriodStartTimeMillis;
    private LevelScoreDiff gracePeriodScoreDiff;

    private final NavigableMap<Long, Score_> scoresByTime;

    public AdaptiveTermination(long gracePeriodMillis, double minimumImprovementRatio) {
        this.gracePeriodMillis = gracePeriodMillis;
        this.minimumImprovementRatio = minimumImprovementRatio;
        this.scoresByTime = new TreeMap<>();
    }

    private record LevelScoreDiff(boolean harderScoreChanged, double softestScoreDiff) {
        /**
         * Calculates the softest score difference between two scores and records
         * if any harder levels changed. Returns null if the score are the same.
         *
         * @param start The first score, typically smaller
         * @param end The second score, typically larger
         * @return A {@link LevelScoreDiff} where harderScoreChanged is true
         *         if and only if a level beside the softest score level changed,
         *         and the difference between the softest score as a double, or null
         *         if both scores are the same.
         * @param <Score_> The score type.
         */
        public static <Score_ extends Score<Score_>> @Nullable LevelScoreDiff between(@NonNull Score_ start,
                @NonNull Score_ end) {
            var scoreDiffs = end.subtract(start).toLevelDoubles();
            var softestLevel = scoreDiffs.length - 1;
            for (int i = 0; i < scoreDiffs.length; i++) {
                if (scoreDiffs[i] != 0.0) {
                    return new LevelScoreDiff(softestLevel != i, scoreDiffs[softestLevel]);
                }
            }
            return null;
        }
    }

    public void start(long startTime, Score_ startingScore) {
        resetGracePeriod(startTime, startingScore);
    }

    public void step(long time, Score_ bestScore) {
        scoresByTime.put(time, bestScore);
    }

    private void resetGracePeriod(long currentTime, Score_ startingScore) {
        gracePeriodStartTimeMillis = currentTime;
        isGracePeriodActive = true;

        // Remove all entries in the map since grace is reset
        scoresByTime.clear();

        // Put the current best score as the first entry
        scoresByTime.put(currentTime, startingScore);
    }

    public boolean isTerminated(long currentTime, Score_ endScore) {
        if (isGracePeriodActive) {
            // first score in scoresByTime = first score in grace period window
            var endpointDiff = LevelScoreDiff.between(scoresByTime.firstEntry().getValue(), endScore);
            var timeElapsedMillis = currentTime - gracePeriodStartTimeMillis;
            if (endpointDiff != null && endpointDiff.harderScoreChanged) {
                // A harder score changed, so reset the grace period
                resetGracePeriod(currentTime, endScore);
                return false;
            }
            if (timeElapsedMillis >= gracePeriodMillis) {
                // grace period over, record the reference diff
                isGracePeriodActive = false;
                gracePeriodScoreDiff = endpointDiff;
                return endpointDiff == null;
            }
            return false;
        }

        var startScoreEntry = scoresByTime.floorEntry(currentTime - gracePeriodMillis);
        var startScore = startScoreEntry.getValue();
        scoresByTime.subMap(0L, startScoreEntry.getKey()).clear();
        var scoreDiff = LevelScoreDiff.between(startScore, endScore);

        if (scoreDiff == null) {
            // no change after grace period, terminate
            return true;
        }

        if (scoreDiff.harderScoreChanged) {
            // A harder score changed, so reset the grace period
            resetGracePeriod(currentTime, endScore);
            return false;
        }

        return scoreDiff.softestScoreDiff / gracePeriodScoreDiff.softestScoreDiff < minimumImprovementRatio;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        return isTerminated(System.currentTimeMillis(), (Score_) solverScope.getBestScore());
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        return isTerminated(System.currentTimeMillis(), phaseScope.getBestScore());
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
        return new AdaptiveTermination<>(gracePeriodMillis, minimumImprovementRatio);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {

    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {

    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        step(System.currentTimeMillis(), stepScope.getPhaseScope().getBestScore());
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {

    }

    @Override
    @SuppressWarnings("unchecked")
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        start(System.currentTimeMillis(), (Score_) solverScope.getBestScore());
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        scoresByTime.clear();
    }
}
