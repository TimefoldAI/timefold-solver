package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.kopt.KOptListMoveSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.DefaultSolver;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Every evaluated k-opt move must change the solution.
 */
@Execution(ExecutionMode.SAME_THREAD)
class KOptListMoveNoOpTest {

    private static List<List<TestdataListValue>> stepState;
    private static long evaluationCount;
    private static long unchangedCount;

    private static List<List<TestdataListValue>> snapshot(TestdataListSolution solution) {
        var result = new ArrayList<List<TestdataListValue>>();
        for (var entity : solution.getEntityList()) {
            result.add(List.copyOf(entity.getValueList()));
        }
        return result;
    }

    public static class UnchangedCountingCalculator implements EasyScoreCalculator<TestdataListSolution, SimpleScore> {
        @Override
        public SimpleScore calculateScore(TestdataListSolution solution) {
            var state = snapshot(solution);
            if (stepState != null) {
                evaluationCount++;
                if (state.equals(stepState)) {
                    unchangedCount++;
                }
            }
            // Distinct states almost always get distinct scores, so the local search keeps moving.
            return SimpleScore.of(state.stream()
                    .map(values -> values.stream().map(TestdataListValue::getCode).toList())
                    .toList()
                    .hashCode());
        }
    }

    @ParameterizedTest
    @CsvSource({ "3, 3, 1", "3, 3, 10", "3, 3, 30", "4, 4, 10", "5, 5, 10", "2, 5, 10", "3, 6, 10" })
    void everyMoveChangesSolution(int minK, int maxK, int entityCount) {
        stepState = null;
        evaluationCount = 0;
        unchangedCount = 0;
        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class)
                .withEasyScoreCalculatorClass(UnchangedCountingCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectorConfig(new KOptListMoveSelectorConfig().withMinimumK(minK).withMaximumK(maxK))
                                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(5_000L)));
        var solver = (DefaultSolver<TestdataListSolution>) SolverFactory.<TestdataListSolution> create(solverConfig)
                .buildSolver();
        solver.addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<>() {
            @Override
            public void stepStarted(AbstractStepScope<TestdataListSolution> stepScope) {
                stepState = stepScope instanceof LocalSearchStepScope<?> ? snapshot(stepScope.getWorkingSolution()) : null;
            }

            @Override
            public void phaseEnded(AbstractPhaseScope<TestdataListSolution> phaseScope) {
                // The phase-end score calculation is not a move evaluation.
                stepState = null;
            }
        });
        solver.solve(TestdataListSolution.generateUninitializedSolution(60, entityCount));
        assertThat(evaluationCount).isGreaterThan(1_000L);
        assertThat(unchangedCount).isZero();
    }
}
