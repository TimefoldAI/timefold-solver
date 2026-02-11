package ai.timefold.solver.benchmark.impl.statistic.movecountperstep;

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

public class MoveCountPerStepProblemStatistic extends ProblemStatistic<LineChart<Long, Long>> {

    private MoveCountPerStepProblemStatistic() {
        // For JAXB.
    }

    public MoveCountPerStepProblemStatistic(ProblemBenchmarkResult problemBenchmarkResult) {
        super(problemBenchmarkResult, ProblemStatisticType.MOVE_COUNT_PER_STEP);
    }

    @Override
    public SubSingleStatistic createSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        return new MoveCountPerStepSubSingleStatistic(subSingleBenchmarkResult);
    }

    @Override
    protected List<LineChart<Long, Long>> generateCharts(BenchmarkReport benchmarkReport) {
        LineChart.Builder<Long, Long> builder = new LineChart.Builder<>();
        for (SingleBenchmarkResult singleBenchmarkResult : problemBenchmarkResult.getSingleBenchmarkResultList()) {
            String acceptedSeriesLabel =
                    singleBenchmarkResult.getSolverBenchmarkResult().getNameWithFavoriteSuffix() + " accepted";
            String selectedSeriesLabel =
                    singleBenchmarkResult.getSolverBenchmarkResult().getNameWithFavoriteSuffix() + " selected";
            if (singleBenchmarkResult.hasAllSuccess()) {
                var subSingleStatistic = singleBenchmarkResult.getSubSingleStatistic(problemStatisticType);
                List<MoveCountPerStepStatisticPoint> list = subSingleStatistic.getPointList();
                for (MoveCountPerStepStatisticPoint point : list) {
                    long timeMillisSpent = point.getTimeMillisSpent();
                    builder.add(acceptedSeriesLabel, timeMillisSpent, point.getAcceptedMoveCount());
                    builder.add(selectedSeriesLabel, timeMillisSpent, point.getSelectedMoveCount());
                }
            }
            if (singleBenchmarkResult.getSolverBenchmarkResult().isFavorite()) {
                builder.markFavorite(acceptedSeriesLabel);
                builder.markFavorite(selectedSeriesLabel);
            }
        }
        return singletonList(builder.build("moveCountPerStepProblemStatisticChart",
                problemBenchmarkResult.getName() + " move count per step statistic", "Time spent", "Moves per step", true, true,
                false));
    }
}
