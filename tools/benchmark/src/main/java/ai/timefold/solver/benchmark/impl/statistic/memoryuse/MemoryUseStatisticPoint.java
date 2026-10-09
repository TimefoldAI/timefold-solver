package ai.timefold.solver.benchmark.impl.statistic.memoryuse;

import ai.timefold.solver.benchmark.impl.statistic.StatisticPoint;

public class MemoryUseStatisticPoint extends StatisticPoint {

    private final long timeMillisSpent;
    private final long usedMemory;
    private final long maxMemory;

    public MemoryUseStatisticPoint(long timeMillisSpent, long usedMemory, long maxMemory) {
        this.timeMillisSpent = timeMillisSpent;
        this.usedMemory = usedMemory;
        this.maxMemory = maxMemory;
    }

    public long getTimeMillisSpent() {
        return timeMillisSpent;
    }

    public long getUsedMemory() {
        return usedMemory;
    }

    public long getMaxMemory() {
        return maxMemory;
    }

    @Override
    public String toCsvLine() {
        return buildCsvLineWithLongs(timeMillisSpent, usedMemory, maxMemory);
    }

}
