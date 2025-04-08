package ai.timefold.solver.benchmark.impl.statistic.stepscore;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.report.LineChart;
import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.ProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.SubSingleStatistic;

public class StepScoreProblemStatistic extends ProblemStatistic<LineChart<Long, Double>> {

    private StepScoreProblemStatistic() {
        // For JAXB.
    }

    public StepScoreProblemStatistic(ProblemBenchmarkResult problemBenchmarkResult) {
        super(problemBenchmarkResult, ProblemStatisticType.STEP_SCORE);
    }

    @Override
    public SubSingleStatistic createSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        return new StepScoreSubSingleStatistic(subSingleBenchmarkResult);
    }

    @Override
    protected List<LineChart<Long, Double>> generateCharts(BenchmarkReport benchmarkReport) {
        var builderList = new ArrayList<LineChart.Builder<Long, Double>>(BenchmarkReport.CHARTED_SCORE_LEVEL_SIZE);
        for (var singleBenchmarkResult : problemBenchmarkResult.getSingleBenchmarkResultList()) {
            var solverLabel = singleBenchmarkResult.getSolverBenchmarkResult().getNameWithFavoriteSuffix();
            // No direct ascending lines between 2 points, but a stepping line instead
            if (singleBenchmarkResult.hasAllSuccess()) {
                var subSingleStatistic = singleBenchmarkResult.getSubSingleStatistic(problemStatisticType);
                List<StepScoreStatisticPoint> points = subSingleStatistic.getPointList();
                for (var point : points) {
                    if (!point.isInitialized()) {
                        continue;
                    }
                    var timeMillisSpent = point.getTimeMillisSpent();
                    var levelValues = point.getScore().toLevelDoubles();
                    for (var i = 0; i < levelValues.length && i < BenchmarkReport.CHARTED_SCORE_LEVEL_SIZE; i++) {
                        if (i >= builderList.size()) {
                            builderList.add(new LineChart.Builder<>());
                        }
                        var builder = builderList.get(i);
                        if (singleBenchmarkResult.getSolverBenchmarkResult().isFavorite()) {
                            builder.markFavorite(solverLabel);
                        }
                        builder.add(solverLabel, timeMillisSpent, levelValues[i]);
                    }
                }
            }
        }
        var chartList = new ArrayList<LineChart<Long, Double>>(BenchmarkReport.CHARTED_SCORE_LEVEL_SIZE);
        for (var scoreLevelIndex = 0; scoreLevelIndex < builderList.size(); scoreLevelIndex++) {
            var scoreLevelLabel = problemBenchmarkResult.findScoreLevelLabel(scoreLevelIndex);
            var chart =
                    builderList.get(scoreLevelIndex).build("stepScoreProblemStatisticChart" + scoreLevelIndex,
                            problemBenchmarkResult.getName() + " step " + scoreLevelLabel + " statistic", "Time spent",
                            "Step " + scoreLevelLabel, true, true, false);
            chartList.add(chart);
        }
        return chartList;
    }
}
