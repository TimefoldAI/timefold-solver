package ai.timefold.solver.benchmark.impl.statistic.movecalculationspeed;

import ai.timefold.solver.benchmark.impl.statistic.StatisticPoint;

public class MoveCalculationSpeedStatisticPoint extends StatisticPoint {

    private final long timeMillisSpent;
    private final long moveCalculationSpeed;

    public MoveCalculationSpeedStatisticPoint(long timeMillisSpent, long moveCalculationSpeed) {
        this.timeMillisSpent = timeMillisSpent;
        this.moveCalculationSpeed = moveCalculationSpeed;
    }

    public long getTimeMillisSpent() {
        return timeMillisSpent;
    }

    public long getMoveCalculationSpeed() {
        return moveCalculationSpeed;
    }

    @Override
    public String toCsvLine() {
        return buildCsvLineWithLongs(timeMillisSpent, moveCalculationSpeed);
    }

}
