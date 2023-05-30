package ai.timefold.solver.benchmark.impl.statistic.memoryuse;

import static java.util.Collections.singletonList;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.report.LineChart;
import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.ProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.SubSingleStatistic;

public class MemoryUseProblemStatistic extends ProblemStatistic<LineChart<Long, Long>> {

    private MemoryUseProblemStatistic() {
        // For JAXB.
    }

    public MemoryUseProblemStatistic(ProblemBenchmarkResult problemBenchmarkResult) {
        super(problemBenchmarkResult, ProblemStatisticType.MEMORY_USE);
    }

    @Override
    public SubSingleStatistic createSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        return new MemoryUseSubSingleStatistic(subSingleBenchmarkResult);
    }

    @Override
    public List<String> getWarningList() {
        if (problemBenchmarkResult.getPlannerBenchmarkResult().hasMultipleParallelBenchmarks()) {
            return Collections.singletonList("This memory use statistic shows the sum of the memory of all benchmarks "
                    + "that ran in parallel, due to parallelBenchmarkCount ("
                    + problemBenchmarkResult.getPlannerBenchmarkResult().getParallelBenchmarkCount() + ").");
        } else {
            return Collections.emptyList();
        }
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
                List<MemoryUseStatisticPoint> points = subSingleStatistic.getPointList();
                for (MemoryUseStatisticPoint point : points) {
                    long timeMillisSpent = point.getTimeMillisSpent();
                    builder.add(solverLabel, timeMillisSpent, point.getUsedMemory() / 1024 / 1024);
                }
            }
        }
        return singletonList(builder.build("memoryUseProblemStatisticChart",
                problemBenchmarkResult.getName() + " memory use statistic", "Time spent", "Memory (MiB)", false, true, false));
    }
}
