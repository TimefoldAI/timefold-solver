package ai.timefold.solver.benchmark.impl.statistic.movecountpertype;

import static java.util.Collections.singletonList;

import java.util.List;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.report.BarChart;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.ProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.SubSingleStatistic;

public class MoveCountPerTypeProblemStatistic extends ProblemStatistic<BarChart<Long>> {
    private MoveCountPerTypeProblemStatistic() {
        // Required by JAXB
    }

    @SuppressWarnings("rawtypes")
    public MoveCountPerTypeProblemStatistic(ProblemBenchmarkResult problemBenchmarkResult) {
        super(problemBenchmarkResult, ProblemStatisticType.MOVE_COUNT_PER_TYPE);
    }

    @SuppressWarnings({ "rawtypes" })
    @Override
    public SubSingleStatistic createSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        return new MoveCountPerTypeSubSingleStatistic(subSingleBenchmarkResult);
    }

    @Override
    protected List<BarChart<Long>> generateCharts(BenchmarkReport benchmarkReport) {
        var builder = new BarChart.Builder<Long>();
        for (var singleBenchmarkResult : problemBenchmarkResult.getSingleBenchmarkResultList()) {
            // No direct ascending lines between 2 points, but a stepping line instead
            if (singleBenchmarkResult.hasAllSuccess()) {
                var solverLabel = singleBenchmarkResult.getSolverBenchmarkResult().getNameWithFavoriteSuffix();
                var subSingleStatistic = singleBenchmarkResult.getSubSingleStatistic(problemStatisticType);
                List<MoveCountPerTypeStatisticPoint> points = subSingleStatistic.getPointList();
                for (var point : points) {
                    builder.add(solverLabel, point.getMoveType(), point.getCount());
                }
            }
        }
        return singletonList(builder.build("moveCountPerTypeProblemStatisticChart",
                problemBenchmarkResult.getName() + " move count per type statistic", "Type", "Count", false));
    }
}
