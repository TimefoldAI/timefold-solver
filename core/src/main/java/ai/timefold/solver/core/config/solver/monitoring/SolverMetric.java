package ai.timefold.solver.core.config.solver.monitoring;

import java.util.function.ToDoubleFunction;

import jakarta.xml.bind.annotation.XmlEnum;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.impl.solver.monitoring.statistic.BestScoreStatistic;
import ai.timefold.solver.core.impl.solver.monitoring.statistic.BestSolutionMutationCountStatistic;
import ai.timefold.solver.core.impl.solver.monitoring.statistic.MemoryUseStatistic;
import ai.timefold.solver.core.impl.solver.monitoring.statistic.MoveCountPerTypeStatistic;
import ai.timefold.solver.core.impl.solver.monitoring.statistic.PickedMoveBestScoreDiffStatistic;
import ai.timefold.solver.core.impl.solver.monitoring.statistic.PickedMoveStepScoreDiffStatistic;
import ai.timefold.solver.core.impl.solver.monitoring.statistic.SolverScopeStatistic;
import ai.timefold.solver.core.impl.solver.monitoring.statistic.SolverStatistic;
import ai.timefold.solver.core.impl.solver.monitoring.statistic.StatelessSolverStatistic;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.jspecify.annotations.NonNull;

@XmlEnum
public enum SolverMetric {

    SOLVE_DURATION("timefold.solver.solve.duration", false),
    ERROR_COUNT("timefold.solver.errors", false),
    SCORE_CALCULATION_COUNT("timefold.solver.score.calculation.count",
            SolverScope::getScoreCalculationCount,
            false),
    MOVE_EVALUATION_COUNT("timefold.solver.move.evaluation.count",
            SolverScope::getMoveEvaluationCount,
            false),
    PROBLEM_ENTITY_COUNT("timefold.solver.problem.entities",
            solverScope -> solverScope.getProblemSizeStatistics().entityCount(),
            false),
    PROBLEM_VARIABLE_COUNT("timefold.solver.problem.variables",
            solverScope -> solverScope.getProblemSizeStatistics().variableCount(),
            false),
    PROBLEM_VALUE_COUNT("timefold.solver.problem.values",
            solverScope -> solverScope.getProblemSizeStatistics().approximateValueCount(),
            false),
    PROBLEM_SIZE_LOG("timefold.solver.problem.size.log",
            solverScope -> solverScope.getProblemSizeStatistics().approximateProblemSizeLog(),
            false),
    BEST_SCORE("timefold.solver.best.score", new BestScoreStatistic<>(), true),
    STEP_SCORE("timefold.solver.step.score", false),
    BEST_SOLUTION_MUTATION("timefold.solver.best.solution.mutation", new BestSolutionMutationCountStatistic<>(), true),
    MOVE_COUNT_PER_STEP("timefold.solver.step.move.count", false),
    MOVE_COUNT_PER_TYPE("timefold.solver.move.type.count", new MoveCountPerTypeStatistic<>(), false),
    MEMORY_USE("jvm.memory.used", new MemoryUseStatistic<>(), false),
    CONSTRAINT_MATCH_TOTAL_BEST_SCORE("timefold.solver.constraint.match.best.score", true, true),
    CONSTRAINT_MATCH_TOTAL_STEP_SCORE("timefold.solver.constraint.match.step.score", false, true),
    PICKED_MOVE_TYPE_BEST_SCORE_DIFF("timefold.solver.move.type.best.score.diff", new PickedMoveBestScoreDiffStatistic<>(),
            true),
    PICKED_MOVE_TYPE_STEP_SCORE_DIFF("timefold.solver.move.type.step.score.diff", new PickedMoveStepScoreDiffStatistic<>(),
            false);

    private final String meterId;
    @SuppressWarnings("rawtypes")
    private final SolverStatistic registerFunction;
    private final boolean isBestSolutionBased;
    private final boolean isConstraintMatchBased;

    SolverMetric(String meterId, boolean isBestSolutionBased) {
        this(meterId, isBestSolutionBased, false);
    }

    SolverMetric(String meterId, boolean isBestSolutionBased, boolean isConstraintMatchBased) {
        this(meterId, new StatelessSolverStatistic<>(), isBestSolutionBased, isConstraintMatchBased);
    }

    SolverMetric(String meterId, ToDoubleFunction<SolverScope<Object>> gaugeFunction, boolean isBestSolutionBased) {
        this(meterId, new SolverScopeStatistic<>(meterId, gaugeFunction), isBestSolutionBased, false);
    }

    SolverMetric(String meterId, SolverStatistic<?> registerFunction, boolean isBestSolutionBased) {
        this(meterId, registerFunction, isBestSolutionBased, false);
    }

    SolverMetric(String meterId, SolverStatistic<?> registerFunction, boolean isBestSolutionBased,
            boolean isConstraintMatchBased) {
        this.meterId = meterId;
        this.registerFunction = registerFunction;
        this.isBestSolutionBased = isBestSolutionBased;
        this.isConstraintMatchBased = isConstraintMatchBased;
    }

    public @NonNull String getMeterId() {
        return meterId;
    }

    public boolean isMetricBestSolutionBased() {
        return isBestSolutionBased;
    }

    public boolean isMetricConstraintMatchBased() {
        return isConstraintMatchBased;
    }

    @SuppressWarnings("unchecked")
    public void register(@NonNull Solver<?> solver) {
        registerFunction.register(solver);
    }

    @SuppressWarnings("unchecked")
    public void unregister(@NonNull Solver<?> solver) {
        registerFunction.unregister(solver);
    }

}
