package ai.timefold.solver.benchmark.impl.statistic.moveevaluationspeed;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.SubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.common.AbstractTimeLineChartProblemStatistic;

public class MoveEvaluationSpeedProblemStatisticTime extends AbstractTimeLineChartProblemStatistic {

    private MoveEvaluationSpeedProblemStatisticTime() {
        // For JAXB.
        this(null);
    }

    public MoveEvaluationSpeedProblemStatisticTime(ProblemBenchmarkResult<?> problemBenchmarkResult) {
        super(ProblemStatisticType.MOVE_EVALUATION_SPEED, problemBenchmarkResult, "moveEvaluationSpeedProblemStatisticChart",
                problemBenchmarkResult.getName() + " move evaluation speed statistic", "Move evaluation speed per second");
    }

    @Override
    public SubSingleStatistic<?, ?> createSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        return new MoveEvaluationSpeedSubSingleStatistic<>(subSingleBenchmarkResult);
    }
}
