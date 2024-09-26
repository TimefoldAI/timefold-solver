package ai.timefold.solver.benchmark.impl.statistic.common;

import ai.timefold.solver.benchmark.impl.statistic.StatisticPoint;

public class LongStatisticPoint extends StatisticPoint {

    private final long timeMillisSpent;
    private final long value;

    public LongStatisticPoint(long timeMillisSpent, long value) {
        this.timeMillisSpent = timeMillisSpent;
        this.value = value;
    }

    public long getTimeMillisSpent() {
        return timeMillisSpent;
    }

    public long getValue() {
        return value;
    }

    @Override
    public String toCsvLine() {
        return buildCsvLineWithLongs(timeMillisSpent, value);
    }

}
