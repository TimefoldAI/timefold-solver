package ai.timefold.solver.benchmark.impl.statistic.scorecalculationspeed;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.SubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.common.AbstractTimeLineChartProblemStatistic;

public class ScoreCalculationSpeedProblemStatistic extends AbstractTimeLineChartProblemStatistic {

    private ScoreCalculationSpeedProblemStatistic() {
        // For JAXB.
        this(null);
    }

    public ScoreCalculationSpeedProblemStatistic(ProblemBenchmarkResult problemBenchmarkResult) {
        super(ProblemStatisticType.SCORE_CALCULATION_SPEED, problemBenchmarkResult,
                "scoreCalculationSpeedProblemStatisticChart",
                problemBenchmarkResult.getName() + " score calculation speed statistic", "Score calculation speed per second");
    }

    @Override
    public SubSingleStatistic createSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        return new ScoreCalculationSpeedSubSingleStatistic(subSingleBenchmarkResult);
    }
}
