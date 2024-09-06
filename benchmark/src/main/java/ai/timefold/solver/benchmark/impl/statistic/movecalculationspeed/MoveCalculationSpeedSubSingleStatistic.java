package ai.timefold.solver.benchmark.impl.statistic.movecalculationspeed;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.StatisticPoint;
import ai.timefold.solver.benchmark.impl.statistic.common.AbstractCalculationSpeedSubSingleStatistic;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;

public class MoveCalculationSpeedSubSingleStatistic<Solution_> extends AbstractCalculationSpeedSubSingleStatistic<Solution_> {

    private MoveCalculationSpeedSubSingleStatistic() {
        // For JAXB.
        this(null);
    }

    public MoveCalculationSpeedSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        this(subSingleBenchmarkResult, 1000L);
    }

    public MoveCalculationSpeedSubSingleStatistic(SubSingleBenchmarkResult benchmarkResult, long timeMillisThresholdInterval) {
        super(SolverMetric.MOVE_CALCULATION_COUNT, ProblemStatisticType.MOVE_CALCULATION_SPEED, benchmarkResult,
                timeMillisThresholdInterval);
    }

    // ************************************************************************
    // CSV methods
    // ************************************************************************

    @Override
    protected String getCsvHeader() {
        return StatisticPoint.buildCsvLine("timeMillisSpent", "moveCalculationSpeed");
    }
}
