package ai.timefold.solver.benchmark.impl.statistic.scorecalculationspeed;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.StatisticPoint;
import ai.timefold.solver.benchmark.impl.statistic.common.AbstractCalculationSpeedSubSingleStatistic;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;

public class ScoreCalculationSpeedSubSingleStatistic<Solution_> extends AbstractCalculationSpeedSubSingleStatistic<Solution_> {

    private ScoreCalculationSpeedSubSingleStatistic() {
        // For JAXB.
        this(null);
    }

    public ScoreCalculationSpeedSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        this(subSingleBenchmarkResult, 1000L);
    }

    public ScoreCalculationSpeedSubSingleStatistic(SubSingleBenchmarkResult benchmarkResult, long timeMillisThresholdInterval) {
        super(SolverMetric.SCORE_CALCULATION_COUNT, ProblemStatisticType.SCORE_CALCULATION_SPEED, benchmarkResult,
                timeMillisThresholdInterval);
    }

    // ************************************************************************
    // CSV methods
    // ************************************************************************

    @Override
    protected String getCsvHeader() {
        return StatisticPoint.buildCsvLine("timeMillisSpent", "scoreCalculationSpeed");
    }
}
