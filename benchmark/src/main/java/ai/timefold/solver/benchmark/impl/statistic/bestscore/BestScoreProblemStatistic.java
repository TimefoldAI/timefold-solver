package ai.timefold.solver.benchmark.impl.statistic.bestscore;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.report.LineChart;
import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.ProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.SubSingleStatistic;

public class BestScoreProblemStatistic extends ProblemStatistic<LineChart<Long, Double>> {

    private BestScoreProblemStatistic() {
        // Required by JAXB
    }

    public BestScoreProblemStatistic(ProblemBenchmarkResult problemBenchmarkResult) {
        super(problemBenchmarkResult, ProblemStatisticType.BEST_SCORE);
    }

    @Override
    public SubSingleStatistic createSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        return new BestScoreSubSingleStatistic<>(subSingleBenchmarkResult);
    }

    @Override
    protected List<LineChart<Long, Double>> generateCharts(BenchmarkReport benchmarkReport) {
        var builderList = new ArrayList<LineChart.Builder<Long, Double>>(BenchmarkReport.CHARTED_SCORE_LEVEL_SIZE);
        for (var singleBenchmarkResult : problemBenchmarkResult.getSingleBenchmarkResultList()) {
            // No direct ascending lines between 2 points, but a stepping line instead
            if (singleBenchmarkResult.hasAllSuccess()) {
                var solverLabel = singleBenchmarkResult.getSolverBenchmarkResult().getNameWithFavoriteSuffix();
                var subSingleStatistic = singleBenchmarkResult.getSubSingleStatistic(problemStatisticType);
                List<BestScoreStatisticPoint> points = subSingleStatistic.getPointList();
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
                // TODO if startingSolution is initialized and no improvement is made, a horizontal line should be shown
                // Draw a horizontal line from the last new best step to how long the solver actually ran
                var timeMillisSpent = singleBenchmarkResult.getTimeMillisSpent();
                var bestScoreLevels = singleBenchmarkResult.getMedian().getScore().toLevelDoubles();
                for (var i = 0; i < bestScoreLevels.length && i < BenchmarkReport.CHARTED_SCORE_LEVEL_SIZE; i++) {
                    if (i >= builderList.size()) {
                        builderList.add(new LineChart.Builder<>());
                    }
                    var builder = builderList.get(i);
                    if (singleBenchmarkResult.getSolverBenchmarkResult().isFavorite()) {
                        builder.markFavorite(solverLabel);
                    }
                    builder.add(solverLabel, timeMillisSpent, bestScoreLevels[i]);
                }
            }
        }
        var chartList = new ArrayList<LineChart<Long, Double>>(builderList.size());
        for (var scoreLevelIndex = 0; scoreLevelIndex < builderList.size(); scoreLevelIndex++) {
            var scoreLevelLabel = problemBenchmarkResult.findScoreLevelLabel(scoreLevelIndex);
            var builder = builderList.get(scoreLevelIndex);
            var chart = builder.build("bestScoreProblemStatisticChart" + scoreLevelIndex,
                    problemBenchmarkResult.getName() + " best " + scoreLevelLabel + " statistic", "Time spent",
                    "Best " + scoreLevelLabel, true, true, false);
            chartList.add(chart);
        }
        return chartList;
    }

}
