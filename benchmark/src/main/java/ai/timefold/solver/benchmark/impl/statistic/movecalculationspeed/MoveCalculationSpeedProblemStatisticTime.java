package ai.timefold.solver.benchmark.impl.statistic.movecalculationspeed;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.SubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.common.AbstractTimeLineChartProblemStatistic;

public class MoveCalculationSpeedProblemStatisticTime extends AbstractTimeLineChartProblemStatistic {

    private MoveCalculationSpeedProblemStatisticTime() {
        // For JAXB.
        this(null);
    }

    public MoveCalculationSpeedProblemStatisticTime(ProblemBenchmarkResult<?> problemBenchmarkResult) {
        super(ProblemStatisticType.MOVE_CALCULATION_SPEED, problemBenchmarkResult, "moveCalculationSpeedProblemStatisticChart",
                problemBenchmarkResult.getName() + " move calculation speed statistic", "Move calculation speed per second");
    }

    @Override
    public SubSingleStatistic<?, ?> createSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        return new MoveCalculationSpeedSubSingleStatistic<>(subSingleBenchmarkResult);
    }
}
