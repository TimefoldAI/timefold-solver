package ai.timefold.solver.benchmark.impl.statistic.bestscore;

import ai.timefold.solver.benchmark.impl.statistic.StatisticPoint;
import ai.timefold.solver.core.api.score.Score;

public class BestScoreStatisticPoint extends StatisticPoint {

    private final long timeMillisSpent;
    private final Score score;
    private final boolean isInitialized;

    public BestScoreStatisticPoint(long timeMillisSpent, Score score, boolean isInitialized) {
        this.timeMillisSpent = timeMillisSpent;
        this.score = score;
        this.isInitialized = isInitialized;
    }

    public long getTimeMillisSpent() {
        return timeMillisSpent;
    }

    public Score getScore() {
        return score;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public String toCsvLine() {
        return buildCsvLineWithStrings(timeMillisSpent, score.toString(), isInitialized ? "true" : "false");
    }

}
