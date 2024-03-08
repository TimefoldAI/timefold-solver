package ai.timefold.solver.core.config.solver.monitoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToDoubleFunction;

import jakarta.xml.bind.annotation.XmlEnum;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.statistic.BestScoreStatistic;
import ai.timefold.solver.core.impl.statistic.BestSolutionMutationCountStatistic;
import ai.timefold.solver.core.impl.statistic.MemoryUseStatistic;
import ai.timefold.solver.core.impl.statistic.PickedMoveBestScoreDiffStatistic;
import ai.timefold.solver.core.impl.statistic.PickedMoveStepScoreDiffStatistic;
import ai.timefold.solver.core.impl.statistic.SolverScopeStatistic;
import ai.timefold.solver.core.impl.statistic.SolverStatistic;
import ai.timefold.solver.core.impl.statistic.StatelessSolverStatistic;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

@XmlEnum
public enum SolverMetric {
    SOLVE_DURATION("timefold.solver.solve.duration", false),
    ERROR_COUNT("timefold.solver.errors", false),
    SCORE_CALCULATION_COUNT("timefold.solver.score.calculation.count",
            SolverScope::getScoreCalculationCount,
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

    public String getMeterId() {
        return meterId;
    }

    public static void registerScoreMetrics(SolverMetric metric, Tags tags, ScoreDefinition<?> scoreDefinition,
            Map<Tags, List<AtomicReference<Number>>> tagToScoreLevels, Score<?> score) {
        Number[] levelValues = score.toLevelNumbers();
        if (tagToScoreLevels.containsKey(tags)) {
            List<AtomicReference<Number>> scoreLevels = tagToScoreLevels.get(tags);
            for (int i = 0; i < levelValues.length; i++) {
                scoreLevels.get(i).set(levelValues[i]);
            }
        } else {
            String[] levelLabels = scoreDefinition.getLevelLabels();
            for (int i = 0; i < levelLabels.length; i++) {
                levelLabels[i] = levelLabels[i].replace(' ', '.');
            }
            List<AtomicReference<Number>> scoreLevels = new ArrayList<>(levelValues.length);
            for (int i = 0; i < levelValues.length; i++) {
                scoreLevels.add(Metrics.gauge(metric.getMeterId() + "." + levelLabels[i],
                        tags, new AtomicReference<>(levelValues[i]),
                        ar -> ar.get().doubleValue()));
            }
            tagToScoreLevels.put(tags, scoreLevels);
        }
    }

    public boolean isMetricBestSolutionBased() {
        return isBestSolutionBased;
    }

    public boolean isMetricConstraintMatchBased() {
        return isConstraintMatchBased;
    }

    @SuppressWarnings("unchecked")
    public void register(Solver<?> solver) {
        registerFunction.register(solver);
    }

    @SuppressWarnings("unchecked")
    public void unregister(Solver<?> solver) {
        registerFunction.unregister(solver);
    }
}
