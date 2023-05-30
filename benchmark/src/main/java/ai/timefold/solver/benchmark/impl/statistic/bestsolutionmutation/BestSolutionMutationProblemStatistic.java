package ai.timefold.solver.benchmark.impl.statistic.bestsolutionmutation;

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

public class BestSolutionMutationProblemStatistic extends ProblemStatistic<LineChart<Long, Long>> {

    private BestSolutionMutationProblemStatistic() {
        // For JAXB.
    }

    public BestSolutionMutationProblemStatistic(ProblemBenchmarkResult problemBenchmarkResult) {
        super(problemBenchmarkResult, ProblemStatisticType.BEST_SOLUTION_MUTATION);
    }

    @Override
    public SubSingleStatistic createSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        return new BestSolutionMutationSubSingleStatistic(subSingleBenchmarkResult);
    }

    @Override
    protected List<LineChart<Long, Long>> generateCharts(BenchmarkReport benchmarkReport) {
        LineChart.Builder<Long, Long> builder = new LineChart.Builder<>();
        for (SingleBenchmarkResult singleBenchmarkResult : problemBenchmarkResult.getSingleBenchmarkResultList()) {
            String solverLabel = singleBenchmarkResult.getSolverBenchmarkResult().getNameWithFavoriteSuffix();
            if (singleBenchmarkResult.getSolverBenchmarkResult().isFavorite()) {
                builder.markFavorite(solverLabel);
            }
            if (singleBenchmarkResult.hasAllSuccess()) {
                var subSingleStatistic = singleBenchmarkResult.getSubSingleStatistic(problemStatisticType);
                List<BestSolutionMutationStatisticPoint> points = subSingleStatistic.getPointList();
                for (BestSolutionMutationStatisticPoint point : points) {
                    long timeMillisSpent = point.getTimeMillisSpent();
                    long mutationCount = point.getMutationCount();
                    builder.add(solverLabel, timeMillisSpent, mutationCount);
                }
            }
        }
        return singletonList(builder.build("bestSolutionMutationProblemStatisticChart",
                problemBenchmarkResult.getName() + " best solution mutation statistic", "Time spent",
                "Best solution mutation count.", true, true, false));
    }
}
