package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.kopt.KOptListMoveSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.decider.forager.LocalSearchForagerConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.TestdataPinnedListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.TestdataPinnedListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.TestdataPinnedListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListValue;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

/**
 * The point of this test is to run enough random K-Opt moves to find out any bugs in the implementation.
 */
@Execution(CONCURRENT)
class KOptListMoveSelectorTest {

    // Pinning the entire entity works; fully pinned are filtered out in the source/destination selectors.
    // But the test still fails, because it generates 5-opt moves where 4 is the max.
    @Test
    void solveWithPlanningListVariableEntityPinFair() {
        var expectedEntityCount = 3;
        var expectedValueCount = expectedEntityCount * 3;
        var solution = TestdataPinnedListSolution.generateUninitializedSolution(expectedValueCount, expectedEntityCount);
        var pinnedEntity = solution.getEntityList().get(0);
        var pinnedList = pinnedEntity.getValueList();
        var pinnedValue = solution.getValueList().get(0);
        pinnedList.add(pinnedValue);
        pinnedEntity.setPinned(true);

        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataPinnedListSolution.class, TestdataPinnedListEntity.class,
                        TestdataPinnedListValue.class)
                .withEasyScoreCalculatorClass(MinimizeUnusedEntitiesEasyScoreCalculator.class);
        solverConfig = decorateSolverConfigWithPhases(solverConfig);
        var solverFactory = SolverFactory.<TestdataPinnedListSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();
        solution = solver.solve(updateSolution(solverFactory, solution));

        assertThat(solution).isNotNull();
        assertThat(solution.getScore()).isEqualTo(SimpleScore.ZERO); // No unused entities.
        assertThat(solution.getEntityList().get(0).getValueList())
                .containsExactly(solution.getValueList().get(0));
        int actualValueCount = solution.getEntityList().stream()
                .mapToInt(e -> e.getValueList().size())
                .sum();
        assertThat(actualValueCount).isEqualTo(expectedValueCount);
    }

    private SolverConfig decorateSolverConfigWithPhases(SolverConfig solverConfig) {
        // Use maxK = 6 so patchCycles is also tested
        return solverConfig
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withPhaseList(
                        List.of(
                                new ConstructionHeuristicPhaseConfig(),
                                // Gives plenty of iterations to find a bug, while still being deterministic and finite.
                                new LocalSearchPhaseConfig()
                                        .withTerminationConfig(
                                                new TerminationConfig()
                                                        .withStepCountLimit(1_00))
                                        .withForagerConfig(new LocalSearchForagerConfig()
                                                .withAcceptedCountLimit(1_00))
                                        .withMoveSelectorConfig(
                                                new KOptListMoveSelectorConfig()
                                                        .withMinimumK(2)
                                                        .withMaximumK(6))));
    }

    private static <Solution_> Solution_ updateSolution(SolverFactory<Solution_> solverFactory, Solution_ solution) {
        SolutionManager<Solution_, ?> solutionManager = SolutionManager.create(solverFactory);
        solutionManager.update(solution);
        return solution;
    }

    // Pinning the entire entity works; fully pinned are filtered out in the source/destination selectors.
    // But the test still fails, because it generates 5-opt moves where 4 is the max.
    @Test
    void solveWithPlanningListVariableEntityPinUnfair() {
        var expectedEntityCount = 3;
        var expectedValueCount = expectedEntityCount * 3;
        var solution = TestdataPinnedListSolution.generateUninitializedSolution(expectedValueCount, expectedEntityCount);
        var pinnedEntity = solution.getEntityList().get(0);
        var pinnedList = pinnedEntity.getValueList();
        var pinnedValue = solution.getValueList().get(0);
        pinnedList.add(pinnedValue);
        pinnedEntity.setPinned(true);

        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataPinnedListSolution.class, TestdataPinnedListEntity.class,
                        TestdataPinnedListValue.class)
                .withEasyScoreCalculatorClass(MaximizeUnusedEntitiesEasyScoreCalculator.class);
        solverConfig = decorateSolverConfigWithPhases(solverConfig);
        var solverFactory = SolverFactory.<TestdataPinnedListSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();
        solution = solver.solve(updateSolution(solverFactory, solution));

        assertThat(solution).isNotNull();
        // 1 unused entity; out of 3 total, one is pinned and the other gets all the values.
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(1));
        assertThat(solution.getEntityList().get(0).getValueList())
                .containsExactly(solution.getValueList().get(0));
        int actualValueCount = solution.getEntityList().stream()
                .mapToInt(e -> e.getValueList().size())
                .sum();
        assertThat(actualValueCount).isEqualTo(expectedValueCount);
    }

    // Broken due to fail-fasts.
    // A: K-Opt goes to k=5 even if maxK=4.
    // (Is this a correct fail-fast? I think it is, but I'm not sure.)
    // B: When A is removed, it still touches pinned indexes.
    // My guess is that B is because K-Opt tries to put things before the pin index
    // as it is joining the end of the list to the beginning.
    @Test
    void solveWithPlanningListVariablePinIndexFair() {
        var expectedEntityCount = 3;
        var expectedValueCount = expectedEntityCount * 3;
        var solution =
                TestdataPinnedWithIndexListSolution.generateUninitializedSolution(expectedValueCount, expectedEntityCount);
        // Pin the first list entirely.
        var pinnedEntity = solution.getEntityList().get(0);
        var pinnedList = pinnedEntity.getValueList();
        var pinnedValue = solution.getValueList().get(0);
        pinnedList.add(pinnedValue);
        pinnedEntity.setPinned(true);
        // In the second list, pin only the first value.
        var partiallyPinnedEntity = solution.getEntityList().get(1);
        var partiallyPinnedList = partiallyPinnedEntity.getValueList();
        var partiallyPinnedValue1 = solution.getValueList().get(1);
        var partiallyPinnedValue2 = solution.getValueList().get(2);
        partiallyPinnedList.add(partiallyPinnedValue1);
        partiallyPinnedList.add(partiallyPinnedValue2);
        partiallyPinnedEntity.setPlanningPinToIndex(1); // The first value is pinned.
        partiallyPinnedEntity.setPinned(false); // The list isn't pinned overall.

        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataPinnedWithIndexListSolution.class, TestdataPinnedWithIndexListEntity.class,
                        TestdataPinnedWithIndexListValue.class)
                .withEasyScoreCalculatorClass(MinimizeUnusedEntitiesEasyScoreCalculator.class);
        solverConfig = decorateSolverConfigWithPhases(solverConfig);
        var solverFactory = SolverFactory.<TestdataPinnedWithIndexListSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();
        solution = solver.solve(updateSolution(solverFactory, solution));

        assertThat(solution).isNotNull();
        // No unused entities.
        assertThat(solution.getScore()).isEqualTo(SimpleScore.ZERO);
        // Entire entity is pinned.
        assertThat(solution.getEntityList().get(0).getValueList()).containsExactly(solution.getValueList().get(0));
        assertThat(solution.getEntityList().get(1).getValueList())
                .first()
                .isEqualTo(solution.getValueList().get(1)); // Pinned to index 0.
        int actualValueCount = solution.getEntityList().stream()
                .mapToInt(e -> e.getValueList().size())
                .sum();
        assertThat(actualValueCount).isEqualTo(expectedValueCount);
    }

    // Broken due to fail-fasts.
    // A: K-Opt goes to k=5 even if maxK=4.
    // (Is this a correct fail-fast? I think it is, but I'm not sure.)
    // B: When A is removed, it still touches pinned indexes.
    // My guess is that B is because K-Opt tries to put things before the pin index
    // as it is joining the end of the list to the beginning.
    @Test
    void solveWithPlanningListVariablePinIndexUnfair() {
        var expectedEntityCount = 3;
        var expectedValueCount = expectedEntityCount * 3;
        var solution =
                TestdataPinnedWithIndexListSolution.generateUninitializedSolution(expectedValueCount, expectedEntityCount);
        // Pin the first list entirely.
        var pinnedEntity = solution.getEntityList().get(0);
        var pinnedList = pinnedEntity.getValueList();
        var pinnedValue = solution.getValueList().get(0);
        pinnedList.add(pinnedValue);
        pinnedEntity.setPinned(true);
        // In the second list, pin only the first value.
        var partiallyPinnedEntity = solution.getEntityList().get(1);
        var partiallyPinnedList = partiallyPinnedEntity.getValueList();
        var partiallyPinnedValue1 = solution.getValueList().get(1);
        var partiallyPinnedValue2 = solution.getValueList().get(2);
        partiallyPinnedList.add(partiallyPinnedValue1);
        partiallyPinnedList.add(partiallyPinnedValue2);
        partiallyPinnedEntity.setPlanningPinToIndex(1); // The first value is pinned.
        partiallyPinnedEntity.setPinned(false); // The list isn't pinned overall.

        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataPinnedWithIndexListSolution.class, TestdataPinnedWithIndexListEntity.class,
                        TestdataPinnedWithIndexListValue.class)
                .withEasyScoreCalculatorClass(MaximizeUnusedEntitiesEasyScoreCalculator.class);
        solverConfig = decorateSolverConfigWithPhases(solverConfig);
        var solverFactory = SolverFactory.<TestdataPinnedWithIndexListSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();
        solution = solver.solve(updateSolution(solverFactory, solution));

        assertThat(solution).isNotNull();
        // 1 unused entity; out of 3 total, one is pinned and the other gets all the values.
        assertThat(solution.getScore()).isNotEqualTo(SimpleScore.ZERO);
        // Entire entity is pinned.
        assertThat(solution.getEntityList().get(0).getValueList()).containsExactly(solution.getValueList().get(0));
        assertThat(solution.getEntityList().get(1).getValueList())
                .first()
                .isEqualTo(solution.getValueList().get(1)); // Pinned to index 0.
        int actualValueCount = solution.getEntityList().stream()
                .mapToInt(e -> e.getValueList().size())
                .sum();
        assertThat(actualValueCount).isEqualTo(expectedValueCount);
    }

    public static final class MinimizeUnusedEntitiesEasyScoreCalculator
            implements EasyScoreCalculator<Object, SimpleScore> {

        @Override
        public SimpleScore calculateScore(Object solution) {
            return new MaximizeUnusedEntitiesEasyScoreCalculator().calculateScore(solution).negate();
        }
    }

    public static final class MaximizeUnusedEntitiesEasyScoreCalculator
            implements EasyScoreCalculator<Object, SimpleScore> {

        @Override
        public SimpleScore calculateScore(Object solution) {
            int unusedEntityCount = 0;
            int entityCount = 0;
            if (solution instanceof TestdataPinnedListSolution testdataPinnedListSolution) {
                for (var entity : testdataPinnedListSolution.getEntityList()) {
                    entityCount++;
                    if (entity.getValueList().isEmpty()) {
                        unusedEntityCount++;
                    }
                }
                return SimpleScore.of(unusedEntityCount);
            } else if (solution instanceof TestdataPinnedWithIndexListSolution testdataPinnedWithIndexListSolution) {
                for (var entity : testdataPinnedWithIndexListSolution.getEntityList()) {
                    entityCount++;
                    if (entity.getValueList().isEmpty()) {
                        unusedEntityCount++;
                    }
                }
            } else {
                throw new UnsupportedOperationException();
            }
            return SimpleScore.of((unusedEntityCount * 100) / entityCount);
        }
    }

}
