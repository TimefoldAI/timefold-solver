package ai.timefold.solver.core.impl.solver.scope;

import org.jspecify.annotations.NullMarked;

/**
 * Totals of a whole solver run, summed over every segment between problem changes.
 * Not thread-safe; only the solver thread writes to it.
 */
@NullMarked
public final class SolverRunStatisticsTracker {

    private long timeMillisSpent = 0L;
    private long scoreCalculationCount = 0L;
    private long moveEvaluationCount = 0L;

    public void solvingStarted() {
        timeMillisSpent = 0L;
        scoreCalculationCount = 0L;
        moveEvaluationCount = 0L;
    }

    public void addSegment(long segmentTimeMillisSpent, long segmentScoreCalculationCount,
            long segmentMoveEvaluationCount) {
        timeMillisSpent += segmentTimeMillisSpent;
        scoreCalculationCount += segmentScoreCalculationCount;
        moveEvaluationCount += segmentMoveEvaluationCount;
    }

    public long getTimeMillisSpent() {
        return timeMillisSpent;
    }

    public long getScoreCalculationCount() {
        return scoreCalculationCount;
    }

    public long getMoveEvaluationCount() {
        return moveEvaluationCount;
    }

}
