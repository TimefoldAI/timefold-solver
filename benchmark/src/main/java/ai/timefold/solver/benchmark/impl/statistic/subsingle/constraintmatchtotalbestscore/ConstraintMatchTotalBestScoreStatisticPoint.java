package ai.timefold.solver.benchmark.impl.statistic.subsingle.constraintmatchtotalbestscore;

import ai.timefold.solver.benchmark.impl.statistic.StatisticPoint;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

public class ConstraintMatchTotalBestScoreStatisticPoint extends StatisticPoint {

    private final long timeMillisSpent;
    private final ConstraintRef constraintRef;
    private final int constraintMatchCount;
    private final Score scoreTotal;

    public ConstraintMatchTotalBestScoreStatisticPoint(long timeMillisSpent, ConstraintRef constraintRef,
            int constraintMatchCount, Score scoreTotal) {
        this.timeMillisSpent = timeMillisSpent;
        this.constraintRef = constraintRef;
        this.constraintMatchCount = constraintMatchCount;
        this.scoreTotal = scoreTotal;
    }

    public long getTimeMillisSpent() {
        return timeMillisSpent;
    }

    public String getConstraintPackage() {
        return constraintRef.packageName();
    }

    public String getConstraintName() {
        return constraintRef.constraintName();
    }

    public int getConstraintMatchCount() {
        return constraintMatchCount;
    }

    public Score getScoreTotal() {
        return scoreTotal;
    }

    public String getConstraintId() {
        return constraintRef.constraintId();
    }

    @Override
    public String toCsvLine() {
        return buildCsvLineWithStrings(timeMillisSpent, getConstraintPackage(), getConstraintName(),
                Integer.toString(constraintMatchCount), scoreTotal.toString());
    }

}
