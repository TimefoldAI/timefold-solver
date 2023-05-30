package ai.timefold.solver.benchmark.impl.statistic.scorecalculationspeed;

import static java.util.Collections.singletonList;

import java.util.List;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.report.LineChart;
import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.ProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.SubSingleStatistic;

public class ScoreCalculationSpeedProblemStatistic extends ProblemStatistic<LineChart<Long, Long>> {

    private ScoreCalculationSpeedProblemStatistic() {
        // For JAXB.
    }

    public ScoreCalculationSpeedProblemStatistic(ProblemBenchmarkResult problemBenchmarkResult) {
        super(problemBenchmarkResult, ProblemStatisticType.SCORE_CALCULATION_SPEED);
    }

    @Override
    public SubSingleStatistic createSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        return new ScoreCalculationSpeedSubSingleStatistic(subSingleBenchmarkResult);
    }

    /**
     * @return never null
     */
    @Override
    protected List<LineChart<Long, Long>> generateCharts(BenchmarkReport benchmarkReport) {
        LineChart.Builder<Long, Long> builder = new LineChart.Builder<>();
        for (SingleBenchmarkResult singleBenchmarkResult : problemBenchmarkResult.getSingleBenchmarkResultList()) {
            String solverLabel = singleBenchmarkResult.getSolverBenchmarkResult().getNameWithFavoriteSuffix();
            if (singleBenchmarkResult.hasAllSuccess()) {
                var subSingleStatistic = singleBenchmarkResult.getSubSingleStatistic(problemStatisticType);
                List<ScoreCalculationSpeedStatisticPoint> points = subSingleStatistic.getPointList();
                for (ScoreCalculationSpeedStatisticPoint point : points) {
                    long timeMillisSpent = point.getTimeMillisSpent();
                    long scoreCalculationSpeed = point.getScoreCalculationSpeed();
                    builder.add(solverLabel, timeMillisSpent, scoreCalculationSpeed);
                }
            }
            if (singleBenchmarkResult.getSolverBenchmarkResult().isFavorite()) {
                builder.markFavorite(solverLabel);
            }
        }
        return singletonList(builder.build("scoreCalculationSpeedProblemStatisticChart",
                problemBenchmarkResult.getName() + " score calculation speed statistic", "Time spent",
                "Score calculation speed per second", false, true, false));
    }
}
