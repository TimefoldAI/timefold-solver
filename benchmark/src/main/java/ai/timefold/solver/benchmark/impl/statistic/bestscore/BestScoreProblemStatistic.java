package ai.timefold.solver.benchmark.impl.statistic.bestscore;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.report.LineChart;
import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SingleBenchmarkResult;
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
        return new BestScoreSubSingleStatistic(subSingleBenchmarkResult);
    }

    @Override
    protected List<LineChart<Long, Double>> generateCharts(BenchmarkReport benchmarkReport) {
        List<LineChart.Builder<Long, Double>> builderList = new ArrayList<>(BenchmarkReport.CHARTED_SCORE_LEVEL_SIZE);
        for (SingleBenchmarkResult singleBenchmarkResult : problemBenchmarkResult.getSingleBenchmarkResultList()) {
            // No direct ascending lines between 2 points, but a stepping line instead
            if (singleBenchmarkResult.hasAllSuccess()) {
                String solverLabel = singleBenchmarkResult.getSolverBenchmarkResult().getNameWithFavoriteSuffix();
                var subSingleStatistic = singleBenchmarkResult.getSubSingleStatistic(problemStatisticType);
                List<BestScoreStatisticPoint> points = subSingleStatistic.getPointList();
                for (BestScoreStatisticPoint point : points) {
                    if (!point.getScore().isSolutionInitialized()) {
                        continue;
                    }
                    long timeMillisSpent = point.getTimeMillisSpent();
                    double[] levelValues = point.getScore().toLevelDoubles();
                    for (int i = 0; i < levelValues.length && i < BenchmarkReport.CHARTED_SCORE_LEVEL_SIZE; i++) {
                        if (i >= builderList.size()) {
                            builderList.add(new LineChart.Builder<>());
                        }
                        LineChart.Builder<Long, Double> builder = builderList.get(i);
                        if (singleBenchmarkResult.getSolverBenchmarkResult().isFavorite()) {
                            builder.markFavorite(solverLabel);
                        }
                        builder.add(solverLabel, timeMillisSpent, levelValues[i]);
                    }
                }
                // TODO if startingSolution is initialized and no improvement is made, a horizontal line should be shown
                // Draw a horizontal line from the last new best step to how long the solver actually ran
                long timeMillisSpent = singleBenchmarkResult.getTimeMillisSpent();
                double[] bestScoreLevels = singleBenchmarkResult.getMedian().getScore().toLevelDoubles();
                for (int i = 0; i < bestScoreLevels.length && i < BenchmarkReport.CHARTED_SCORE_LEVEL_SIZE; i++) {
                    if (i >= builderList.size()) {
                        builderList.add(new LineChart.Builder<>());
                    }
                    LineChart.Builder<Long, Double> builder = builderList.get(i);
                    if (singleBenchmarkResult.getSolverBenchmarkResult().isFavorite()) {
                        builder.markFavorite(solverLabel);
                    }
                    builder.add(solverLabel, timeMillisSpent, bestScoreLevels[i]);
                }
            }
        }
        List<LineChart<Long, Double>> chartList = new ArrayList<>(builderList.size());
        for (int scoreLevelIndex = 0; scoreLevelIndex < builderList.size(); scoreLevelIndex++) {
            String scoreLevelLabel = problemBenchmarkResult.findScoreLevelLabel(scoreLevelIndex);
            LineChart.Builder<Long, Double> builder = builderList.get(scoreLevelIndex);
            LineChart<Long, Double> chart = builder.build("bestScoreProblemStatisticChart" + scoreLevelIndex,
                    problemBenchmarkResult.getName() + " best " + scoreLevelLabel + " statistic", "Time spent",
                    "Best " + scoreLevelLabel, true, true, false);
            chartList.add(chart);
        }
        return chartList;
    }

}
