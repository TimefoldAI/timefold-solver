package ai.timefold.solver.benchmark.impl.statistic.movecountpertype;

import java.util.List;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.ProblemBasedSubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.StatisticPoint;
import ai.timefold.solver.benchmark.impl.statistic.StatisticRegistry;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;

import io.micrometer.core.instrument.Tags;

public class MoveCountPerTypeSubSingleStatistic<Solution_>
        extends ProblemBasedSubSingleStatistic<Solution_, MoveCountPerTypeStatisticPoint> {

    MoveCountPerTypeSubSingleStatistic() {
        // For JAXB.
    }

    public MoveCountPerTypeSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        super(subSingleBenchmarkResult, ProblemStatisticType.MOVE_COUNT_PER_TYPE);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void open(StatisticRegistry<Solution_> registry, Tags runTag) {
        registry.addListener(solverScope -> registry.extractMoveCountPerType(solverScope,
                (type, count) -> pointList.add(new MoveCountPerTypeStatisticPoint(type, count))));
    }

    // ************************************************************************
    // CSV methods
    // ************************************************************************

    @Override
    protected String getCsvHeader() {
        return StatisticPoint.buildCsvLine("_", "type", "count");
    }

    @Override
    protected MoveCountPerTypeStatisticPoint createPointFromCsvLine(ScoreDefinition<?> scoreDefinition,
            List<String> csvLine) {
        return new MoveCountPerTypeStatisticPoint(csvLine.get(1), Long.parseLong(csvLine.get(2)));
    }

}
