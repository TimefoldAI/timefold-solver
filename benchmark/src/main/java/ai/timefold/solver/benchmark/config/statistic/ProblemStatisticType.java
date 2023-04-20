package ai.timefold.solver.benchmark.config.statistic;

import java.util.List;

import javax.xml.bind.annotation.XmlEnum;

import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.ProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.StatisticType;
import ai.timefold.solver.benchmark.impl.statistic.bestscore.BestScoreProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.bestsolutionmutation.BestSolutionMutationProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.memoryuse.MemoryUseProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.movecountperstep.MoveCountPerStepProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.scorecalculationspeed.ScoreCalculationSpeedProblemStatistic;
import ai.timefold.solver.benchmark.impl.statistic.stepscore.StepScoreProblemStatistic;

@XmlEnum
public enum ProblemStatisticType implements StatisticType {
    BEST_SCORE,
    STEP_SCORE,
    SCORE_CALCULATION_SPEED,
    BEST_SOLUTION_MUTATION,
    MOVE_COUNT_PER_STEP,
    MEMORY_USE;

    public ProblemStatistic buildProblemStatistic(ProblemBenchmarkResult problemBenchmarkResult) {
        switch (this) {
            case BEST_SCORE:
                return new BestScoreProblemStatistic(problemBenchmarkResult);
            case STEP_SCORE:
                return new StepScoreProblemStatistic(problemBenchmarkResult);
            case SCORE_CALCULATION_SPEED:
                return new ScoreCalculationSpeedProblemStatistic(problemBenchmarkResult);
            case BEST_SOLUTION_MUTATION:
                return new BestSolutionMutationProblemStatistic(problemBenchmarkResult);
            case MOVE_COUNT_PER_STEP:
                return new MoveCountPerStepProblemStatistic(problemBenchmarkResult);
            case MEMORY_USE:
                return new MemoryUseProblemStatistic(problemBenchmarkResult);
            default:
                throw new IllegalStateException("The problemStatisticType (" + this + ") is not implemented.");
        }
    }

    public boolean hasScoreLevels() {
        return this == BEST_SCORE
                || this == STEP_SCORE;
    }

    public static List<ProblemStatisticType> defaultList() {
        return List.of(ProblemStatisticType.BEST_SCORE);
    }

}
