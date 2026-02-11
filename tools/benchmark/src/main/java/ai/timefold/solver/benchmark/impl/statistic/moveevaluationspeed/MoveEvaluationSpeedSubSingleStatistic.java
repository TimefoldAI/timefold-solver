package ai.timefold.solver.benchmark.impl.statistic.moveevaluationspeed;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.StatisticPoint;
import ai.timefold.solver.benchmark.impl.statistic.common.AbstractCalculationSpeedSubSingleStatistic;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;

public class MoveEvaluationSpeedSubSingleStatistic<Solution_> extends AbstractCalculationSpeedSubSingleStatistic<Solution_> {

    private MoveEvaluationSpeedSubSingleStatistic() {
        // For JAXB.
        this(null);
    }

    public MoveEvaluationSpeedSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        this(subSingleBenchmarkResult, 1000L);
    }

    public MoveEvaluationSpeedSubSingleStatistic(SubSingleBenchmarkResult benchmarkResult, long timeMillisThresholdInterval) {
        super(SolverMetric.MOVE_EVALUATION_COUNT, ProblemStatisticType.MOVE_EVALUATION_SPEED, benchmarkResult,
                timeMillisThresholdInterval);
    }

    // ************************************************************************
    // CSV methods
    // ************************************************************************

    @Override
    protected String getCsvHeader() {
        return StatisticPoint.buildCsvLine("timeMillisSpent", "moveEvaluationSpeed");
    }
}
