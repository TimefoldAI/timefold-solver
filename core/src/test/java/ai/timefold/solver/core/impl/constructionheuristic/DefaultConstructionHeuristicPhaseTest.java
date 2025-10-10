package ai.timefold.solver.core.impl.constructionheuristic;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicType;
import ai.timefold.solver.core.config.constructionheuristic.decider.forager.ConstructionHeuristicForagerConfig;
import ai.timefold.solver.core.config.constructionheuristic.decider.forager.ConstructionHeuristicPickEarlyType;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedValuePlacerConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.sort.comparator.OneValuePerEntityEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.list.sort.comparator.TestdataListSortableEntity;
import ai.timefold.solver.core.testdomain.list.sort.comparator.TestdataListSortableSolution;
import ai.timefold.solver.core.testdomain.list.sort.comparator.TestdataListSortableValue;
import ai.timefold.solver.core.testdomain.list.sort.factory.OneValuePerEntityFactoryEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.list.sort.factory.TestdataListFactorySortableEntity;
import ai.timefold.solver.core.testdomain.list.sort.factory.TestdataListFactorySortableSolution;
import ai.timefold.solver.core.testdomain.list.sort.factory.TestdataListFactorySortableValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingScoreCalculator;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;
import ai.timefold.solver.core.testdomain.list.valuerange.sort.comparator.OneValuePerEntityRangeEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.list.valuerange.sort.comparator.TestdataListSortableEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.sort.comparator.TestdataListSortableEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.sort.comparator.TestdataListSortableEntityProvidingValue;
import ai.timefold.solver.core.testdomain.list.valuerange.sort.factory.OneValuePerEntityRangeFactoryEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.list.valuerange.sort.factory.TestdataListFactorySortableEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.sort.factory.TestdataListFactorySortableEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.sort.factory.TestdataListFactorySortableEntityProvidingValue;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedEntity;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedOtherValue;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedSolution;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedValue;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingScoreCalculator;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Execution(ExecutionMode.CONCURRENT)
class DefaultConstructionHeuristicPhaseTest {

    @Test
    void solveWithInitializedEntities() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        var solution = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Arrays.asList(
                new TestdataEntity("e1", null),
                new TestdataEntity("e2", v2),
                new TestdataEntity("e3", v1)));

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
        var solvedE1 = solution.getEntityList().get(0);
        assertCode("e1", solvedE1);
        assertThat(solvedE1.getValue()).isNotNull();
        var solvedE2 = solution.getEntityList().get(1);
        assertCode("e2", solvedE2);
        assertThat(solvedE2.getValue()).isEqualTo(v2);
        var solvedE3 = solution.getEntityList().get(2);
        assertCode("e3", solvedE3);
        assertThat(solvedE3.getValue()).isEqualTo(v1);
    }

    @Test
    void solveWithInitializedSolution() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        var inputProblem = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        inputProblem.setValueList(Arrays.asList(v1, v2, v3));
        inputProblem.setEntityList(Arrays.asList(
                new TestdataEntity("e1", v1),
                new TestdataEntity("e2", v2),
                new TestdataEntity("e3", v3)));

        var solution = PlannerTestUtils.solve(solverConfig, inputProblem, true);
        // Although the solution has not changed, it is a clone since the initial solution
        // may have stale shadow variables.
        assertThat(inputProblem).isNotSameAs(solution);
    }

    @Test
    void solveWithPinnedEntities() {
        var solverConfig =
                PlannerTestUtils.buildSolverConfig(TestdataPinnedSolution.class, TestdataPinnedEntity.class)
                        .withPhases(new ConstructionHeuristicPhaseConfig());

        var solution = new TestdataPinnedSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Arrays.asList(
                new TestdataPinnedEntity("e1", null, false, false),
                new TestdataPinnedEntity("e2", v2, true, false),
                new TestdataPinnedEntity("e3", v3, false, true)));

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
        var solvedE1 = solution.getEntityList().get(0);
        assertCode("e1", solvedE1);
        assertThat(solvedE1.getValue()).isNotNull();
        var solvedE2 = solution.getEntityList().get(1);
        assertCode("e2", solvedE2);
        assertThat(solvedE2.getValue()).isEqualTo(v2);
        var solvedE3 = solution.getEntityList().get(2);
        assertCode("e3", solvedE3);
        assertThat(solvedE3.getValue()).isEqualTo(v3);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.ZERO);
    }

    @Test
    void solveWithPinnedEntitiesWhenUnassignedAllowedAndPinnedToNull() {
        var solverConfig =
                PlannerTestUtils.buildSolverConfig(TestdataPinnedAllowsUnassignedSolution.class,
                        TestdataPinnedAllowsUnassignedEntity.class)
                        .withPhases(new ConstructionHeuristicPhaseConfig());

        var solution = new TestdataPinnedAllowsUnassignedSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Arrays.asList(
                new TestdataPinnedAllowsUnassignedEntity("e1", null, false, false),
                new TestdataPinnedAllowsUnassignedEntity("e2", v2, true, false),
                new TestdataPinnedAllowsUnassignedEntity("e3", null, false, true)));

        solution = PlannerTestUtils.solve(solverConfig, solution, true); // No change will be made, but shadow variables will be updated.
        assertThat(solution).isNotNull();
        assertThat(solution.getScore()).isEqualTo(SimpleScore.ZERO);
    }

    @Test
    void solveWithPinnedEntitiesWhenUnassignedNotAllowedAndPinnedToNull() {
        var solverConfig =
                PlannerTestUtils.buildSolverConfig(TestdataPinnedSolution.class, TestdataPinnedEntity.class)
                        .withPhases(new ConstructionHeuristicPhaseConfig());

        var solution = new TestdataPinnedSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Arrays.asList(
                new TestdataPinnedEntity("e1", null, false, false),
                new TestdataPinnedEntity("e2", v2, true, false),
                new TestdataPinnedEntity("e3", null, false, true)));

        assertThatThrownBy(() -> PlannerTestUtils.solve(solverConfig, solution))
                .hasMessageContaining("entity (e3)")
                .hasMessageContaining("variable (value");
    }

    @Test
    void solveWithEmptyEntityList() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        var solution = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Collections.emptyList());

        solution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(solution).isNotNull();
        assertThat(solution.getEntityList()).isEmpty();
    }

    @Test
    void solveWithAllowsUnassignedBasicVariable() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataAllowsUnassignedSolution.class,
                TestdataAllowsUnassignedEntity.class)
                .withEasyScoreCalculatorClass(TestdataAllowsUnassignedEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");
        var entity = new TestdataAllowsUnassignedEntity("e1");
        entity.setValue(value1);
        var entity2 = new TestdataAllowsUnassignedEntity("e2");
        var entity3 = new TestdataAllowsUnassignedEntity("e3");

        var solution = new TestdataAllowsUnassignedSolution();
        solution.setEntityList(List.of(entity, entity2, entity3));
        solution.setValueList(Arrays.asList(value1, value2));

        var bestSolution = PlannerTestUtils.solve(solverConfig, solution);
        assertSoftly(softly -> {
            softly.assertThat(bestSolution.getScore())
                    .isEqualTo(SimpleScore.of(-1)); // No value assigned twice, null once.
            var firstEntity = bestSolution.getEntityList().get(0);
            var firstValue = bestSolution.getValueList().get(0);
            softly.assertThat(firstEntity.getValue())
                    .isEqualTo(firstValue);
            var secondEntity = bestSolution.getEntityList().get(1);
            var secondValue = bestSolution.getValueList().get(1);
            softly.assertThat(secondEntity.getValue())
                    .isEqualTo(secondValue);
            var thirdEntity = bestSolution.getEntityList().get(2);
            softly.assertThat(thirdEntity.getValue())
                    .isNull();
        });

    }

    @Test
    void solveWithAllowsUnassignedValuesListVariable() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataAllowsUnassignedValuesListSolution.class,
                TestdataAllowsUnassignedValuesListEntity.class, TestdataAllowsUnassignedValuesListValue.class)
                .withEasyScoreCalculatorClass(TestdataAllowsUnassignedValuesListEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        var value1 = new TestdataAllowsUnassignedValuesListValue("v1");
        var value2 = new TestdataAllowsUnassignedValuesListValue("v2");
        var value3 = new TestdataAllowsUnassignedValuesListValue("v3");
        var value4 = new TestdataAllowsUnassignedValuesListValue("v4");
        var entity = TestdataAllowsUnassignedValuesListEntity.createWithValues("e1", value1, value2);

        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(Arrays.asList(value1, value2, value3, value4));

        var bestSolution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertSoftly(softly -> {
            softly.assertThat(bestSolution.getScore())
                    .isEqualTo(SimpleScore.of(-2)); // Length of the entity's value list.
            var firstEntity = bestSolution.getEntityList().get(0);
            var firstValue = bestSolution.getValueList().get(0);
            var secondValue = bestSolution.getValueList().get(1);
            softly.assertThat(firstEntity.getValueList())
                    .containsExactly(firstValue, secondValue);
        });

    }

    @Test
    void solveWithEntityValueRangeBasicVariable() {
        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataAllowsUnassignedEntityProvidingSolution.class,
                        TestdataAllowsUnassignedEntityProvidingEntity.class)
                .withEasyScoreCalculatorClass(TestdataAllowsUnassignedEntityProvidingScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");
        var value3 = new TestdataValue("v3");
        var entity1 = new TestdataAllowsUnassignedEntityProvidingEntity("e1", List.of(value1, value2));
        var entity2 = new TestdataAllowsUnassignedEntityProvidingEntity("e2", List.of(value3));

        var solution = new TestdataAllowsUnassignedEntityProvidingSolution();
        solution.setEntityList(List.of(entity1, entity2));

        var bestSolution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(bestSolution).isNotNull();
        // Only one entity should provide the value list and assign the values.
        assertThat(bestSolution.getEntityList().get(0).getValue()).isNotNull();
        assertThat(bestSolution.getEntityList().get(1).getValue()).isSameAs(value3);
    }

    @Test
    void solveWithEntityValueRangeListVariable() {
        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataListEntityProvidingSolution.class, TestdataListEntityProvidingEntity.class,
                        TestdataListEntityProvidingValue.class)
                .withEasyScoreCalculatorClass(TestdataListEntityProvidingScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        var value1 = new TestdataListEntityProvidingValue("v1");
        var value2 = new TestdataListEntityProvidingValue("v2");
        var value3 = new TestdataListEntityProvidingValue("v3");
        var entity1 = new TestdataListEntityProvidingEntity("e1", List.of(value1, value2));
        var entity2 = new TestdataListEntityProvidingEntity("e2", List.of(value2, value3));

        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(entity1, entity2));

        var bestSolution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(bestSolution).isNotNull();
        // Only one entity should provide the value list and assign the values.
        assertThat(bestSolution.getEntityList().get(0).getValueList().stream().map(TestdataListEntityProvidingValue::getCode))
                .hasSameElementsAs(List.of("v1", "v2"));
        assertThat(bestSolution.getEntityList().get(1).getValueList().stream().map(TestdataListEntityProvidingValue::getCode))
                .hasSameElementsAs(List.of("v3"));
    }

    @Test
    void constructionHeuristicAllocateToValueFromQueue() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig()
                        .withConstructionHeuristicType(ConstructionHeuristicType.ALLOCATE_TO_VALUE_FROM_QUEUE));

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1")));

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getValue() == null)).isEmpty();
    }

    private static List<ConstructionHeuristicTestConfig> generateConstructionHeuristicSimpleConfiguration() {
        var values = new ArrayList<ConstructionHeuristicTestConfig>();
        values.add(new ConstructionHeuristicTestConfig(
                new ConstructionHeuristicPhaseConfig()
                        .withConstructionHeuristicType(ConstructionHeuristicType.FIRST_FIT_DECREASING)
                        .withForagerConfig(new ConstructionHeuristicForagerConfig().withPickEarlyType(
                                ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD)),
                // the entities are being read in decreasing order of difficulty,
                // this is expected: e1[3], e2[2], and e3[1]
                new int[] { 2, 1, 0 },
                // Only the entities are sorted, and shuffling the values will alter the expected result
                false));
        values.add(new ConstructionHeuristicTestConfig(
                new ConstructionHeuristicPhaseConfig()
                        .withConstructionHeuristicType(ConstructionHeuristicType.WEAKEST_FIT)
                        .withForagerConfig(new ConstructionHeuristicForagerConfig().withPickEarlyType(
                                ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD)),
                // the values are being read in increase order of strength,
                // this is expected: e1[1], e2[2], and e3[3]
                new int[] { 0, 1, 2 },
                // Only the values are sorted, and shuffling the entities will alter the expected result
                false));
        values.add(new ConstructionHeuristicTestConfig(
                new ConstructionHeuristicPhaseConfig()
                        .withConstructionHeuristicType(ConstructionHeuristicType.WEAKEST_FIT_DECREASING)
                        .withForagerConfig(new ConstructionHeuristicForagerConfig().withPickEarlyType(
                                ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD)),
                // the entities are being read in decreasing order of difficulty,
                // and the values are being read in increase order of strength
                // this is expected: e1[3], e2[2], and e3[1]
                new int[] { 2, 1, 0 },
                // Both are sorted and the expected result won't be affected
                true));
        values.add(new ConstructionHeuristicTestConfig(
                new ConstructionHeuristicPhaseConfig()
                        .withConstructionHeuristicType(ConstructionHeuristicType.STRONGEST_FIT)
                        .withForagerConfig(new ConstructionHeuristicForagerConfig().withPickEarlyType(
                                ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD)),
                // and the values are being read in decreasing order of strength
                // this is expected: e1[3], e2[2], and e3[1]
                new int[] { 2, 1, 0 },
                // Only the values are sorted, and shuffling the entities will alter the expected result
                false));
        values.add(new ConstructionHeuristicTestConfig(
                new ConstructionHeuristicPhaseConfig()
                        .withConstructionHeuristicType(ConstructionHeuristicType.STRONGEST_FIT_DECREASING)
                        .withForagerConfig(new ConstructionHeuristicForagerConfig().withPickEarlyType(
                                ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD)),
                // the entities are being read in decreasing order of difficulty,
                // and the values are being read in decreasing order of strength
                // this is expected: e1[1], e2[2], and e3[3]
                new int[] { 0, 1, 2 },
                // Both are sorted and the expected result won't be affected
                true));
        // Allocate from pool
        // Simple configuration
        values.add(new ConstructionHeuristicTestConfig(
                new ConstructionHeuristicPhaseConfig()
                        .withConstructionHeuristicType(ConstructionHeuristicType.ALLOCATE_TO_VALUE_FROM_QUEUE)
                        .withEntitySorterManner(EntitySorterManner.DECREASING_DIFFICULTY)
                        .withValueSorterManner(ValueSorterManner.DECREASING_STRENGTH)
                        .withForagerConfig(new ConstructionHeuristicForagerConfig().withPickEarlyType(
                                ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD)),
                // Since we are starting from decreasing strength
                // and the entities are being read in decreasing order of difficulty,
                // this is expected: e1[1], e2[2], and e3[3]
                new int[] { 0, 1, 2 },
                // Both are sorted and the expected result won't be affected
                true));
        values.add(new ConstructionHeuristicTestConfig(
                new ConstructionHeuristicPhaseConfig()
                        .withConstructionHeuristicType(ConstructionHeuristicType.ALLOCATE_TO_VALUE_FROM_QUEUE)
                        .withEntitySorterManner(EntitySorterManner.DECREASING_DIFFICULTY_IF_AVAILABLE)
                        .withValueSorterManner(ValueSorterManner.DECREASING_STRENGTH_IF_AVAILABLE)
                        .withForagerConfig(new ConstructionHeuristicForagerConfig().withPickEarlyType(
                                ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD)),
                // this is expected: e1[1], e2[2], and e3[3]
                new int[] { 0, 1, 2 },
                // Both are sorted and the expected result won't be affected
                true));
        values.add(new ConstructionHeuristicTestConfig(
                new ConstructionHeuristicPhaseConfig()
                        .withConstructionHeuristicType(ConstructionHeuristicType.ALLOCATE_TO_VALUE_FROM_QUEUE)
                        .withEntitySorterManner(EntitySorterManner.NONE)
                        .withValueSorterManner(ValueSorterManner.DECREASING_STRENGTH)
                        .withForagerConfig(new ConstructionHeuristicForagerConfig().withPickEarlyType(
                                ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD)),
                // this is expected: e1[3], e2[2], and e3[1]
                new int[] { 2, 1, 0 },
                // Only the values are sorted, and shuffling the entities will alter the expected result
                false));
        values.add(new ConstructionHeuristicTestConfig(
                new ConstructionHeuristicPhaseConfig()
                        .withConstructionHeuristicType(ConstructionHeuristicType.ALLOCATE_TO_VALUE_FROM_QUEUE)
                        .withEntitySorterManner(EntitySorterManner.DECREASING_DIFFICULTY)
                        .withValueSorterManner(ValueSorterManner.INCREASING_STRENGTH)
                        .withForagerConfig(new ConstructionHeuristicForagerConfig().withPickEarlyType(
                                ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD)),
                // this is expected: e1[3], e2[2], and e3[1]
                new int[] { 2, 1, 0 },
                // Both are sorted and the expected result won't be affected
                true));
        values.add(new ConstructionHeuristicTestConfig(
                new ConstructionHeuristicPhaseConfig()
                        .withConstructionHeuristicType(ConstructionHeuristicType.ALLOCATE_TO_VALUE_FROM_QUEUE)
                        .withEntitySorterManner(EntitySorterManner.DECREASING_DIFFICULTY_IF_AVAILABLE)
                        .withValueSorterManner(ValueSorterManner.INCREASING_STRENGTH_IF_AVAILABLE)
                        .withForagerConfig(new ConstructionHeuristicForagerConfig().withPickEarlyType(
                                ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD)),
                // this is expected: e1[3], e2[2], and e3[1]
                new int[] { 2, 1, 0 },
                // Both are sorted and the expected result won't be affected
                true));
        values.add(new ConstructionHeuristicTestConfig(
                new ConstructionHeuristicPhaseConfig()
                        .withConstructionHeuristicType(ConstructionHeuristicType.ALLOCATE_TO_VALUE_FROM_QUEUE)
                        .withEntitySorterManner(EntitySorterManner.NONE)
                        .withValueSorterManner(ValueSorterManner.INCREASING_STRENGTH)
                        .withForagerConfig(new ConstructionHeuristicForagerConfig().withPickEarlyType(
                                ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD)),
                // this is expected: e1[1], e2[2], and e3[3]
                new int[] { 0, 1, 2 },
                // Only the values are sorted, and shuffling the entities will alter the expected result
                false));
        return values;
    }

    private static List<ConstructionHeuristicTestConfig>
            generateConstructionHeuristicAdvancedConfiguration(SelectionCacheType entityDestinationCacheType) {
        var values = new ArrayList<ConstructionHeuristicTestConfig>();
        // Advanced configuration
        values.add(new ConstructionHeuristicTestConfig(
                new ConstructionHeuristicPhaseConfig()
                        .withEntityPlacerConfig(new QueuedValuePlacerConfig()
                                .withValueSelectorConfig(new ValueSelectorConfig()
                                        .withId("sortedValueSelector")
                                        .withSelectionOrder(SelectionOrder.SORTED)
                                        .withCacheType(SelectionCacheType.PHASE)
                                        .withSorterManner(ValueSorterManner.DECREASING_STRENGTH))
                                .withMoveSelectorConfig(new ListChangeMoveSelectorConfig()
                                        .withValueSelectorConfig(
                                                new ValueSelectorConfig().withMimicSelectorRef("sortedValueSelector"))
                                        .withDestinationSelectorConfig(new DestinationSelectorConfig()
                                                .withValueSelectorConfig(new ValueSelectorConfig())
                                                .withEntitySelectorConfig(new EntitySelectorConfig()
                                                        .withSelectionOrder(SelectionOrder.SORTED)
                                                        .withCacheType(entityDestinationCacheType)
                                                        .withSorterManner(EntitySorterManner.DECREASING_DIFFICULTY)))))
                        .withForagerConfig(new ConstructionHeuristicForagerConfig().withPickEarlyType(
                                ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD)),
                // Since we are starting from decreasing strength
                // and the entities are being read in decreasing order of difficulty,
                // this is expected: e1[1], e2[2], and e3[3]
                new int[] { 0, 1, 2 },
                // Both are sorted and the expected result won't be affected
                true));
        var nonSortedEntityConfig = new EntitySelectorConfig();
        var isPhaseScope = entityDestinationCacheType == SelectionCacheType.PHASE;
        if (isPhaseScope) {
            // Hack to prevent the default sorting option,
            // which is DECREASING_DIFFICULTY_IF_AVAILABLE
            // This hack does not work with STEP scope
            nonSortedEntityConfig.setSorterManner(EntitySorterManner.NONE);
            nonSortedEntityConfig.setSelectionOrder(SelectionOrder.SORTED);
            nonSortedEntityConfig.setCacheType(entityDestinationCacheType);
        }
        values.add(new ConstructionHeuristicTestConfig(
                new ConstructionHeuristicPhaseConfig()
                        .withEntityPlacerConfig(new QueuedValuePlacerConfig()
                                .withValueSelectorConfig(new ValueSelectorConfig()
                                        .withId("sortedValueSelector")
                                        .withSelectionOrder(SelectionOrder.SORTED)
                                        .withCacheType(SelectionCacheType.PHASE)
                                        .withSorterManner(ValueSorterManner.DECREASING_STRENGTH))
                                .withMoveSelectorConfig(new ListChangeMoveSelectorConfig()
                                        .withValueSelectorConfig(
                                                new ValueSelectorConfig().withMimicSelectorRef("sortedValueSelector"))
                                        .withDestinationSelectorConfig(new DestinationSelectorConfig()
                                                .withValueSelectorConfig(new ValueSelectorConfig())
                                                .withEntitySelectorConfig(nonSortedEntityConfig))))
                        .withForagerConfig(new ConstructionHeuristicForagerConfig().withPickEarlyType(
                                ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD)),
                // this is expected: e1[3], e2[2], and e3[1]
                // The step scope will apply the default entity sort manner
                isPhaseScope ? new int[] { 2, 1, 0 } : new int[] { 0, 1, 2 },
                // Only the values are sorted, and shuffling the entities will alter the expected result
                false));
        values.add(new ConstructionHeuristicTestConfig(
                new ConstructionHeuristicPhaseConfig()
                        .withEntityPlacerConfig(new QueuedValuePlacerConfig()
                                .withValueSelectorConfig(new ValueSelectorConfig()
                                        .withId("sortedValueSelector")
                                        .withSelectionOrder(SelectionOrder.SORTED)
                                        .withCacheType(SelectionCacheType.PHASE)
                                        .withSorterManner(ValueSorterManner.INCREASING_STRENGTH))
                                .withMoveSelectorConfig(new ListChangeMoveSelectorConfig()
                                        .withValueSelectorConfig(
                                                new ValueSelectorConfig().withMimicSelectorRef("sortedValueSelector"))
                                        .withDestinationSelectorConfig(new DestinationSelectorConfig()
                                                .withValueSelectorConfig(new ValueSelectorConfig())
                                                .withEntitySelectorConfig(new EntitySelectorConfig()
                                                        .withSelectionOrder(SelectionOrder.SORTED)
                                                        .withCacheType(entityDestinationCacheType)
                                                        .withSorterManner(EntitySorterManner.DECREASING_DIFFICULTY)))))
                        .withForagerConfig(new ConstructionHeuristicForagerConfig().withPickEarlyType(
                                ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD)),
                // this is expected: e1[3], e2[2], and e3[1]
                new int[] { 2, 1, 0 },
                // Both are sorted and the expected result won't be affected
                true));
        values.add(new ConstructionHeuristicTestConfig(
                new ConstructionHeuristicPhaseConfig()
                        .withEntityPlacerConfig(new QueuedValuePlacerConfig()
                                .withValueSelectorConfig(new ValueSelectorConfig()
                                        .withId("sortedValueSelector")
                                        .withSelectionOrder(SelectionOrder.SORTED)
                                        .withCacheType(SelectionCacheType.PHASE)
                                        .withSorterManner(ValueSorterManner.INCREASING_STRENGTH))
                                .withMoveSelectorConfig(new ListChangeMoveSelectorConfig()
                                        .withValueSelectorConfig(
                                                new ValueSelectorConfig().withMimicSelectorRef("sortedValueSelector"))
                                        .withDestinationSelectorConfig(new DestinationSelectorConfig()
                                                .withValueSelectorConfig(new ValueSelectorConfig())
                                                .withEntitySelectorConfig(nonSortedEntityConfig))))
                        .withForagerConfig(new ConstructionHeuristicForagerConfig().withPickEarlyType(
                                ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD)),
                // this is expected: e1[1], e2[2], and e3[3]
                // The step scope will apply the default entity sort manner
                isPhaseScope ? new int[] { 0, 1, 2 } : new int[] { 2, 1, 0 },
                // Only the values are sorted, and shuffling the entities will alter the expected result
                false));
        return values;
    }

    private static List<ConstructionHeuristicTestConfig> generateConstructionHeuristicConfiguration() {
        var values = new ArrayList<ConstructionHeuristicTestConfig>();
        values.addAll(generateConstructionHeuristicSimpleConfiguration());
        values.addAll(generateConstructionHeuristicAdvancedConfiguration(SelectionCacheType.PHASE));
        return values;
    }

    @ParameterizedTest
    @MethodSource("generateConstructionHeuristicConfiguration")
    void constructionHeuristicListVarAllocateValueFromQueueComparator(ConstructionHeuristicTestConfig phaseConfig) {
        var solverConfig =
                PlannerTestUtils
                        .buildSolverConfig(TestdataListSortableSolution.class, TestdataListSortableEntity.class,
                                TestdataListSortableValue.class)
                        .withEasyScoreCalculatorClass(OneValuePerEntityEasyScoreCalculator.class)
                        .withPhases(phaseConfig.config());

        var solution = TestdataListSortableSolution.generateSolution(3, 3, phaseConfig.shuffle());

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
        if (phaseConfig.expected() != null) {
            for (var i = 0; i < 3; i++) {
                var id = "Generated Entity %d".formatted(i);
                var entity = solution.getEntityList().stream()
                        .filter(e -> e.getCode().equals(id))
                        .findFirst()
                        .orElseThrow(IllegalArgumentException::new);
                assertThat(entity.getValueList()).hasSize(1);
                assertThat(entity.getValueList().get(0).getStrength()).isEqualTo(phaseConfig.expected[i]);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("generateConstructionHeuristicConfiguration")
    void constructionHeuristicListVarAllocateValueFromQueueFactory(ConstructionHeuristicTestConfig phaseConfig) {
        var solverConfig =
                PlannerTestUtils
                        .buildSolverConfig(TestdataListFactorySortableSolution.class, TestdataListFactorySortableEntity.class,
                                TestdataListFactorySortableValue.class)
                        .withEasyScoreCalculatorClass(OneValuePerEntityFactoryEasyScoreCalculator.class)
                        .withPhases(phaseConfig.config());

        var solution = TestdataListFactorySortableSolution.generateSolution(3, 3, phaseConfig.shuffle());

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
        if (phaseConfig.expected() != null) {
            for (var i = 0; i < 3; i++) {
                var id = "Generated Entity %d".formatted(i);
                var entity = solution.getEntityList().stream()
                        .filter(e -> e.getCode().equals(id))
                        .findFirst()
                        .orElseThrow(IllegalArgumentException::new);
                assertThat(entity.getValueList()).hasSize(1);
                assertThat(entity.getValueList().get(0).getStrength()).isEqualTo(phaseConfig.expected[i]);
            }
        }
    }

    private static List<ConstructionHeuristicTestConfig> generateEntityRangeConstructionHeuristicConfiguration() {
        var values = new ArrayList<ConstructionHeuristicTestConfig>();
        values.addAll(generateConstructionHeuristicSimpleConfiguration());
        values.addAll(generateConstructionHeuristicAdvancedConfiguration(SelectionCacheType.STEP));
        return values;
    }

    @ParameterizedTest
    @MethodSource("generateEntityRangeConstructionHeuristicConfiguration")
    void constructionHeuristicListVarEntityRangeAllocateToValueFromQueueComparator(
            ConstructionHeuristicTestConfig phaseConfig) {
        var solverConfig =
                PlannerTestUtils
                        .buildSolverConfig(TestdataListSortableEntityProvidingSolution.class,
                                TestdataListSortableEntityProvidingEntity.class,
                                TestdataListSortableEntityProvidingValue.class)
                        .withEasyScoreCalculatorClass(OneValuePerEntityRangeEasyScoreCalculator.class)
                        .withPhases(phaseConfig.config());

        var solution = TestdataListSortableEntityProvidingSolution.generateSolution(3, 3, phaseConfig.shuffle());

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
        if (phaseConfig.expected() != null) {
            for (var i = 0; i < 3; i++) {
                var id = "Generated Entity %d".formatted(i);
                var entity = solution.getEntityList().stream()
                        .filter(e -> e.getCode().equals(id))
                        .findFirst()
                        .orElseThrow(IllegalArgumentException::new);
                assertThat(entity.getValueList()).hasSize(1);
                assertThat(entity.getValueList().get(0).getStrength()).isEqualTo(phaseConfig.expected[i]);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("generateEntityRangeConstructionHeuristicConfiguration")
    void constructionHeuristicListVarEntityRangeAllocateToValueFromQueueFactory(ConstructionHeuristicTestConfig phaseConfig) {
        var solverConfig =
                PlannerTestUtils
                        .buildSolverConfig(TestdataListFactorySortableEntityProvidingSolution.class,
                                TestdataListFactorySortableEntityProvidingEntity.class,
                                TestdataListFactorySortableEntityProvidingValue.class)
                        .withEasyScoreCalculatorClass(OneValuePerEntityRangeFactoryEasyScoreCalculator.class)
                        .withPhases(phaseConfig.config());

        var solution = TestdataListFactorySortableEntityProvidingSolution.generateSolution(3, 3,
                phaseConfig.shuffle());

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
        if (phaseConfig.expected() != null) {
            for (var i = 0; i < 3; i++) {
                var id = "Generated Entity %d".formatted(i);
                var entity = solution.getEntityList().stream()
                        .filter(e -> e.getCode().equals(id))
                        .findFirst()
                        .orElseThrow(IllegalArgumentException::new);
                assertThat(entity.getValueList()).hasSize(1);
                assertThat(entity.getValueList().get(0).getStrength()).isEqualTo(phaseConfig.expected[i]);
            }
        }
    }

    @Test
    void failConstructionHeuristicEntityRange() {
        var solverConfig =
                PlannerTestUtils
                        .buildSolverConfig(TestdataListSortableEntityProvidingSolution.class,
                                TestdataListSortableEntityProvidingEntity.class,
                                TestdataListSortableEntityProvidingValue.class)
                        .withEasyScoreCalculatorClass(OneValuePerEntityRangeEasyScoreCalculator.class)
                        .withPhases(
                                new ConstructionHeuristicPhaseConfig()
                                        .withEntityPlacerConfig(new QueuedValuePlacerConfig()
                                                .withValueSelectorConfig(new ValueSelectorConfig()
                                                        .withId("sortedValueSelector")
                                                        .withSelectionOrder(SelectionOrder.SORTED)
                                                        .withCacheType(SelectionCacheType.PHASE)
                                                        .withSorterManner(ValueSorterManner.DECREASING_STRENGTH))
                                                .withMoveSelectorConfig(new ListChangeMoveSelectorConfig()
                                                        .withValueSelectorConfig(
                                                                new ValueSelectorConfig()
                                                                        .withMimicSelectorRef("sortedValueSelector"))
                                                        .withDestinationSelectorConfig(new DestinationSelectorConfig()
                                                                .withValueSelectorConfig(new ValueSelectorConfig())
                                                                .withEntitySelectorConfig(new EntitySelectorConfig()
                                                                        .withSelectionOrder(SelectionOrder.SORTED)
                                                                        .withCacheType(SelectionCacheType.PHASE)
                                                                        .withSorterManner(
                                                                                EntitySorterManner.DECREASING_DIFFICULTY))))));
        var solution = TestdataListSortableEntityProvidingSolution.generateSolution(3, 3, true);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, solution))
                .hasMessageContaining("resolvedSelectionOrder (SORTED) which does not support the resolvedCacheType (PHASE)")
                .hasMessageContaining("Maybe set the \"cacheType\" to STEP.");
    }

    @Test
    void failMixedModelDefaultConfiguration() {
        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataMixedSolution.class, TestdataMixedEntity.class, TestdataMixedValue.class,
                        TestdataMixedOtherValue.class);

        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, new TestdataSolution("s1")))
                .hasMessageContaining(
                        "has both basic and list variables and cannot be deduced automatically");
    }

    private record ConstructionHeuristicTestConfig(ConstructionHeuristicPhaseConfig config, int[] expected, boolean shuffle) {

    }
}
