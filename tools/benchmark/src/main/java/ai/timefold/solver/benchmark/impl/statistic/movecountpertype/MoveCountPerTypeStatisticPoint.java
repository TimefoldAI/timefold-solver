package ai.timefold.solver.benchmark.impl.statistic.movecountpertype;

import ai.timefold.solver.benchmark.impl.statistic.StatisticPoint;

public class MoveCountPerTypeStatisticPoint extends StatisticPoint {

    private final String moveType;
    private final long count;

    public MoveCountPerTypeStatisticPoint(String moveType, long count) {
        this.moveType = moveType;
        this.count = count;
    }

    public String getMoveType() {
        return moveType;
    }

    public long getCount() {
        return count;
    }

    @Override
    public String toCsvLine() {
        return buildCsvLineWithStrings(0L, moveType, String.valueOf(count));
    }

}
