package ai.timefold.solver.benchmark.impl.statistic.stepscore;

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
        List<LineChart.Builder<Long, Double>> builderList = new ArrayList<>(BenchmarkReport.CHARTED_SCORE_LEVEL_SIZE);
        for (SingleBenchmarkResult singleBenchmarkResult : problemBenchmarkResult.getSingleBenchmarkResultList()) {
            String solverLabel = singleBenchmarkResult.getSolverBenchmarkResult().getNameWithFavoriteSuffix();
            // No direct ascending lines between 2 points, but a stepping line instead
            if (singleBenchmarkResult.hasAllSuccess()) {
                var subSingleStatistic = singleBenchmarkResult.getSubSingleStatistic(problemStatisticType);
                List<StepScoreStatisticPoint> points = subSingleStatistic.getPointList();
                for (StepScoreStatisticPoint point : points) {
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
            }
        }
        List<LineChart<Long, Double>> chartList = new ArrayList<>(BenchmarkReport.CHARTED_SCORE_LEVEL_SIZE);
        for (int scoreLevelIndex = 0; scoreLevelIndex < builderList.size(); scoreLevelIndex++) {
            String scoreLevelLabel = problemBenchmarkResult.findScoreLevelLabel(scoreLevelIndex);
            LineChart<Long, Double> chart =
                    builderList.get(scoreLevelIndex).build("stepScoreProblemStatisticChart" + scoreLevelIndex,
                            problemBenchmarkResult.getName() + " step " + scoreLevelLabel + " statistic", "Time spent",
                            "Step " + scoreLevelLabel, true, true, false);
            chartList.add(chart);
        }
        return chartList;
    }
}
