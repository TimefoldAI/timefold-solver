package ai.timefold.solver.benchmark.impl.statistic.movecalculationspeed;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.ProblemBasedSubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.StatisticPoint;
import ai.timefold.solver.benchmark.impl.statistic.StatisticRegistry;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;

import io.micrometer.core.instrument.Tags;

public class MoveCalculationSpeedSubSingleStatistic<Solution_>
        extends ProblemBasedSubSingleStatistic<Solution_, MoveCalculationSpeedStatisticPoint> {

    private long timeMillisThresholdInterval;

    private MoveCalculationSpeedSubSingleStatistic() {
        // For JAXB.
    }

    public MoveCalculationSpeedSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        this(subSingleBenchmarkResult, 1000L);
    }

    public MoveCalculationSpeedSubSingleStatistic(SubSingleBenchmarkResult benchmarkResult, long timeMillisThresholdInterval) {
        super(benchmarkResult, ProblemStatisticType.MOVE_CALCULATION_SPEED);
        if (timeMillisThresholdInterval <= 0L) {
            throw new IllegalArgumentException("The timeMillisThresholdInterval (" + timeMillisThresholdInterval
                    + ") must be bigger than 0.");
        }
        this.timeMillisThresholdInterval = timeMillisThresholdInterval;
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void open(StatisticRegistry<Solution_> registry, Tags runTag) {
        registry.addListener(SolverMetric.MOVE_CALCULATION_COUNT, new Consumer<>() {
            long nextTimeMillisThreshold = timeMillisThresholdInterval;
            long lastTimeMillisSpent = 0L;
            final AtomicLong lastMoveCalculationCount = new AtomicLong(0);

            @Override
            public void accept(Long timeMillisSpent) {
                if (timeMillisSpent >= nextTimeMillisThreshold) {
                    registry.getGaugeValue(SolverMetric.MOVE_CALCULATION_COUNT, runTag, moveCalculationCountNumber -> {
                        var moveCalculationCount = moveCalculationCountNumber.longValue();
                        var calculationCountInterval = moveCalculationCount - lastMoveCalculationCount.get();
                        var timeMillisSpentInterval = timeMillisSpent - lastTimeMillisSpent;
                        if (timeMillisSpentInterval == 0L) {
                            // Avoid divide by zero exception on a fast CPU
                            timeMillisSpentInterval = 1L;
                        }
                        var moveCalculationSpeed = calculationCountInterval * 1000L / timeMillisSpentInterval;
                        pointList.add(new MoveCalculationSpeedStatisticPoint(timeMillisSpent, moveCalculationSpeed));
                        lastMoveCalculationCount.set(moveCalculationCount);
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
    protected String getCsvHeader() {
        return StatisticPoint.buildCsvLine("timeMillisSpent", "moveCalculationSpeed");
    }

    @Override
    protected MoveCalculationSpeedStatisticPoint createPointFromCsvLine(ScoreDefinition<?> scoreDefinition,
            List<String> csvLine) {
        return new MoveCalculationSpeedStatisticPoint(Long.parseLong(csvLine.get(0)),
                Long.parseLong(csvLine.get(1)));
    }

}
