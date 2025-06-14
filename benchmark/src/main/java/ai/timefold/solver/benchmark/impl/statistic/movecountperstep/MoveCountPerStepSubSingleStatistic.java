package ai.timefold.solver.benchmark.impl.statistic.movecountperstep;

import java.util.List;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.ProblemBasedSubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.StatisticPoint;
import ai.timefold.solver.benchmark.impl.statistic.StatisticRegistry;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.solver.monitoring.SolverMetricUtil;

import io.micrometer.core.instrument.Tags;

public class MoveCountPerStepSubSingleStatistic<Solution_>
        extends ProblemBasedSubSingleStatistic<Solution_, MoveCountPerStepStatisticPoint> {

    private MoveCountPerStepSubSingleStatistic() {
        // For JAXB.
    }

    public MoveCountPerStepSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        super(subSingleBenchmarkResult, ProblemStatisticType.MOVE_COUNT_PER_STEP);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void open(StatisticRegistry<Solution_> registry, Tags runTag) {
        registry.addListener(SolverMetric.MOVE_COUNT_PER_STEP,
                timeMillisSpent -> {
                    var accepted = SolverMetricUtil.getGaugeValue(registry,
                            SolverMetric.MOVE_COUNT_PER_STEP.getMeterId() + ".accepted", runTag);
                    if (accepted != null) {
                        var selected = SolverMetricUtil.getGaugeValue(registry,
                                SolverMetric.MOVE_COUNT_PER_STEP.getMeterId() + ".selected", runTag);
                        if (selected != null) {
                            pointList.add(new MoveCountPerStepStatisticPoint(timeMillisSpent, accepted.longValue(),
                                    selected.longValue()));
                        }
                    }
                });
    }

    // ************************************************************************
    // CSV methods
    // ************************************************************************

    @Override
    protected String getCsvHeader() {
        return StatisticPoint.buildCsvLine("timeMillisSpent", "acceptedMoveCount", "selectedMoveCount");
    }

    @Override
    protected MoveCountPerStepStatisticPoint createPointFromCsvLine(ScoreDefinition<?> scoreDefinition,
            List<String> csvLine) {
        return new MoveCountPerStepStatisticPoint(Long.parseLong(csvLine.get(0)), Long.parseLong(csvLine.get(1)),
                Long.parseLong(csvLine.get(2)));
    }

}
