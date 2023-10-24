package ai.timefold.solver.benchmark.impl.statistic.bestscore;

import java.util.List;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.ProblemBasedSubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.StatisticPoint;
import ai.timefold.solver.benchmark.impl.statistic.StatisticRegistry;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;

import io.micrometer.core.instrument.Tags;

public class BestScoreSubSingleStatistic<Solution_>
        extends ProblemBasedSubSingleStatistic<Solution_, BestScoreStatisticPoint> {

    BestScoreSubSingleStatistic() {
        // For JAXB.
    }

    public BestScoreSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        super(subSingleBenchmarkResult, ProblemStatisticType.BEST_SCORE);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void open(StatisticRegistry<Solution_> registry, Tags runTag) {
        registry.addListener(SolverMetric.BEST_SCORE,
                timestamp -> registry.extractScoreFromMeters(SolverMetric.BEST_SCORE, runTag,
                        score -> pointList.add(new BestScoreStatisticPoint(timestamp, score))));
    }

    // ************************************************************************
    // CSV methods
    // ************************************************************************

    @Override
    protected String getCsvHeader() {
        return StatisticPoint.buildCsvLine("timeMillisSpent", "score");
    }

    @Override
    protected BestScoreStatisticPoint createPointFromCsvLine(ScoreDefinition<?> scoreDefinition,
            List<String> csvLine) {
        return new BestScoreStatisticPoint(Long.parseLong(csvLine.get(0)),
                scoreDefinition.parseScore(csvLine.get(1)));
    }

}
