package ai.timefold.solver.benchmark.impl.statistic.common;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.ProblemBasedSubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.StatisticRegistry;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;

import io.micrometer.core.instrument.Tags;

public abstract class AbstractCalculationSpeedSubSingleStatistic<Solution_>
        extends ProblemBasedSubSingleStatistic<Solution_, LongStatisticPoint> {

    private final SolverMetric solverMetric;
    private final long timeMillisThresholdInterval;

    protected AbstractCalculationSpeedSubSingleStatistic(SolverMetric solverMetric, ProblemStatisticType statisticType,
            SubSingleBenchmarkResult benchmarkResult, long timeMillisThresholdInterval) {
        super(benchmarkResult, statisticType);
        if (timeMillisThresholdInterval <= 0L) {
            throw new IllegalArgumentException("The timeMillisThresholdInterval (" + timeMillisThresholdInterval
                    + ") must be bigger than 0.");
        }
        this.solverMetric = solverMetric;
        this.timeMillisThresholdInterval = timeMillisThresholdInterval;
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void open(StatisticRegistry<Solution_> registry, Tags runTag) {
        registry.addListener(solverMetric, new Consumer<>() {
            long nextTimeMillisThreshold = timeMillisThresholdInterval;
            long lastTimeMillisSpent = 0L;
            final AtomicLong lastCalculationCount = new AtomicLong(0);

            @Override
            public void accept(Long timeMillisSpent) {
                if (timeMillisSpent >= nextTimeMillisThreshold) {
                    registry.getGaugeValue(solverMetric, runTag, countNumber -> {
                        var moveEvaluationCount = countNumber.longValue();
                        var countInterval = moveEvaluationCount - lastCalculationCount.get();
                        var timeMillisSpentInterval = timeMillisSpent - lastTimeMillisSpent;
                        if (timeMillisSpentInterval == 0L) {
                            // Avoid divide by zero exception on a fast CPU
                            timeMillisSpentInterval = 1L;
                        }
                        var speed = countInterval * 1000L / timeMillisSpentInterval;
                        pointList.add(new LongStatisticPoint(timeMillisSpent, speed));
                        lastCalculationCount.set(moveEvaluationCount);
                    });
                    lastTimeMillisSpent = timeMillisSpent;
                    nextTimeMillisThreshold += timeMillisThresholdInterval;
                    if (nextTimeMillisThreshold < timeMillisSpent) {
                        nextTimeMillisThreshold = timeMillisSpent;
                    }
                }
            }
        });
    }

    // ************************************************************************
    // CSV methods
    // ************************************************************************

    @Override
    protected LongStatisticPoint createPointFromCsvLine(ScoreDefinition<?> scoreDefinition,
            List<String> csvLine) {
        return new LongStatisticPoint(Long.parseLong(csvLine.get(0)), Long.parseLong(csvLine.get(1)));
    }
}
