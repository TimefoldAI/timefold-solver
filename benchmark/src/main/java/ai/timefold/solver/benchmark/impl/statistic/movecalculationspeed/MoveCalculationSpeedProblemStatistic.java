package ai.timefold.solver.benchmark.impl.statistic.movecalculationspeed;

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

public class MoveCalculationSpeedProblemStatistic extends ProblemStatistic<LineChart<Long, Long>> {

    private MoveCalculationSpeedProblemStatistic() {
        // For JAXB.
    }

    public MoveCalculationSpeedProblemStatistic(ProblemBenchmarkResult<?> problemBenchmarkResult) {
        super(problemBenchmarkResult, ProblemStatisticType.MOVE_CALCULATION_SPEED);
    }

    @Override
    public SubSingleStatistic<?, ?> createSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        return new MoveCalculationSpeedSubSingleStatistic<>(subSingleBenchmarkResult);
    }

    /**
     * @return never null
     */
    @Override
    protected List<LineChart<Long, Long>> generateCharts(BenchmarkReport benchmarkReport) {
        var builder = new LineChart.Builder<Long, Long>();
        for (var singleBenchmarkResult : problemBenchmarkResult.getSingleBenchmarkResultList()) {
            var solverLabel = singleBenchmarkResult.getSolverBenchmarkResult().getNameWithFavoriteSuffix();
            if (singleBenchmarkResult.hasAllSuccess()) {
                var subSingleStatistic = singleBenchmarkResult.getSubSingleStatistic(problemStatisticType);
                List<MoveCalculationSpeedStatisticPoint> points = subSingleStatistic.getPointList();
                for (var point : points) {
                    var timeMillisSpent = point.getTimeMillisSpent();
                    var moveCalculationSpeed = point.getMoveCalculationSpeed();
                    builder.add(solverLabel, timeMillisSpent, moveCalculationSpeed);
                }
            }
            if (singleBenchmarkResult.getSolverBenchmarkResult().isFavorite()) {
                builder.markFavorite(solverLabel);
            }
        }
        return singletonList(builder.build("moveCalculationSpeedProblemStatisticChart",
                problemBenchmarkResult.getName() + " move calculation speed statistic", "Time spent",
                "Move calculation speed per second", false, true, false));
    }
}
