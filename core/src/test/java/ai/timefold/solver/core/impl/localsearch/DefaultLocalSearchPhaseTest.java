package ai.timefold.solver.core.impl.localsearch;

import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchType;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedEntity;
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.externalized.TestdataListEntityExternalized;
import ai.timefold.solver.core.impl.testdata.domain.list.externalized.TestdataListSolutionExternalized;
import ai.timefold.solver.core.impl.testdata.domain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.impl.testdata.domain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.impl.testdata.domain.pinned.allows_unassigned.TestdataPinnedAllowsUnassignedEntity;
import ai.timefold.solver.core.impl.testdata.domain.pinned.allows_unassigned.TestdataPinnedAllowsUnassignedSolution;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class DefaultLocalSearchPhaseTest {

    @Test
    void solveWithInitializedEntities() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        var phaseConfig = new LocalSearchPhaseConfig();
        phaseConfig.setTerminationConfig(new TerminationConfig().withScoreCalculationCountLimit(10L));
        solverConfig.setPhaseConfigList(Collections.singletonList(phaseConfig));

        var solution = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Arrays.asList(
                new TestdataEntity("e1", v1),
                new TestdataEntity("e2", v2),
                new TestdataEntity("e3", v1)));

        solution = PlannerTestUtils.solve(solverConfig, solution, false); // TODO incentive it to change something
        assertThat(solution).isNotNull();
        var solvedE1 = solution.getEntityList().get(0);
        assertCode("e1", solvedE1);
        assertThat(solvedE1.getValue()).isNotNull();
        var solvedE2 = solution.getEntityList().get(1);
        assertCode("e2", solvedE2);
        assertThat(solvedE2.getValue()).isNotNull();
        var solvedE3 = solution.getEntityList().get(2);
        assertCode("e3", solvedE3);
        assertThat(solvedE3.getValue()).isNotNull();
    }

    @Test
    void solveWithPinnedEntities() {
        var solverConfig =
                PlannerTestUtils.buildSolverConfig(TestdataPinnedSolution.class, TestdataPinnedEntity.class)
                        .withPhases(new LocalSearchPhaseConfig()
                                .withTerminationConfig(new TerminationConfig().withScoreCalculationCountLimit(10L)));

        var solution = new TestdataPinnedSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Arrays.asList(
                new TestdataPinnedEntity("e1", v1, false, false),
                new TestdataPinnedEntity("e2", v2, true, false),
                new TestdataPinnedEntity("e3", v3, false, true)));

        solution = PlannerTestUtils.solve(solverConfig, solution, false); // TODO incentive it to change something
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
    }

    @Test
    void solveWithPinnedEntitiesWhenUnassignedAllowedAndPinnedToNull() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataPinnedAllowsUnassignedSolution.class,
                TestdataPinnedAllowsUnassignedEntity.class)
                .withPhases(new LocalSearchPhaseConfig()
                        .withTerminationConfig(new TerminationConfig().withScoreCalculationCountLimit(10L)));

        var solution = new TestdataPinnedAllowsUnassignedSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Arrays.asList(
                new TestdataPinnedAllowsUnassignedEntity("e1", null, false, false),
                new TestdataPinnedAllowsUnassignedEntity("e2", v2, true, false),
                new TestdataPinnedAllowsUnassignedEntity("e3", null, false, true)));

        solution = PlannerTestUtils.solve(solverConfig, solution, false); // No change will be made.
        assertThat(solution).isNotNull();
        assertThat(solution.getScore()).isEqualTo(SimpleScore.ZERO);
    }

    @Test
    void solveWithPinnedEntitiesWhenUnassignedNotAllowedAndPinnedToNull() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataPinnedSolution.class, TestdataPinnedEntity.class)
                .withPhases(new LocalSearchPhaseConfig()
                        .withTerminationConfig(new TerminationConfig().withScoreCalculationCountLimit(10L)));

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
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        var phaseConfig = new LocalSearchPhaseConfig();
        phaseConfig.setTerminationConfig(new TerminationConfig().withScoreCalculationCountLimit(10L));
        solverConfig.setPhaseConfigList(Collections.singletonList(phaseConfig));

        var solution = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Collections.emptyList());

        solution = PlannerTestUtils.solve(solverConfig, solution, false);
        assertThat(solution).isNotNull();
        assertThat(solution.getEntityList()).isEmpty();
    }

    @Test
    void solveTabuSearchWithInitializedEntities() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        var phaseConfig = new LocalSearchPhaseConfig();
        phaseConfig.setLocalSearchType(LocalSearchType.TABU_SEARCH);
        phaseConfig.setTerminationConfig(new TerminationConfig().withScoreCalculationCountLimit(10L));
        solverConfig.setPhaseConfigList(Collections.singletonList(phaseConfig));

        var solution = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Arrays.asList(
                new TestdataEntity("e1", v1),
                new TestdataEntity("e2", v2),
                new TestdataEntity("e3", v1)));

        solution = PlannerTestUtils.solve(solverConfig, solution, false); // TODO incentive it to change something
        assertThat(solution).isNotNull();
        var solvedE1 = solution.getEntityList().get(0);
        assertCode("e1", solvedE1);
        assertThat(solvedE1.getValue()).isNotNull();
        var solvedE2 = solution.getEntityList().get(1);
        assertCode("e2", solvedE2);
        assertThat(solvedE2.getValue()).isNotNull();
        var solvedE3 = solution.getEntityList().get(2);
        assertCode("e3", solvedE3);
        assertThat(solvedE3.getValue()).isNotNull();
    }

    @Test
    void solveTabuSearchWithPinnedEntities() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataPinnedSolution.class, TestdataPinnedEntity.class);
        var phaseConfig = new LocalSearchPhaseConfig();
        phaseConfig.setLocalSearchType(LocalSearchType.TABU_SEARCH);
        phaseConfig.setTerminationConfig(new TerminationConfig().withScoreCalculationCountLimit(10L));
        solverConfig.setPhaseConfigList(Collections.singletonList(phaseConfig));

        var solution = new TestdataPinnedSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Arrays.asList(
                new TestdataPinnedEntity("e1", v1, false, false),
                new TestdataPinnedEntity("e2", v2, true, false),
                new TestdataPinnedEntity("e3", v3, false, true)));

        solution = PlannerTestUtils.solve(solverConfig, solution, false); // TODO incentive it to change something
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
    }

    @Test
    void solveTabuSearchWithEmptyEntityList() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        var phaseConfig = new LocalSearchPhaseConfig();
        phaseConfig.setLocalSearchType(LocalSearchType.TABU_SEARCH);
        phaseConfig.setTerminationConfig(new TerminationConfig().withScoreCalculationCountLimit(10L));
        solverConfig.setPhaseConfigList(Collections.singletonList(phaseConfig));

        var solution = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Collections.emptyList());

        solution = PlannerTestUtils.solve(solverConfig, solution, false);
        assertThat(solution).isNotNull();
        assertThat(solution.getEntityList()).isEmpty();
    }

    @Test
    void solveListVariable() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class);

        var solution = TestdataListSolution.generateUninitializedSolution(6, 2);

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
    }

    @Test
    void solveMultiVarChainedVariable() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataChainedSolution.class, TestdataChainedEntity.class);

        var solution = TestdataChainedSolution.generateUninitializedSolution(6, 2);

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
    }

    @Test
    void solveListVariableWithExternalizedInverseAndIndexSupplies() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataListSolutionExternalized.class, TestdataListEntityExternalized.class);

        var solution = TestdataListSolutionExternalized.generateUninitializedSolution(6, 2);

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
    }

    @Test
    void failsFastWithUninitializedSolutionBasicVariable() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        solverConfig.withPhases(solverConfig.getPhaseConfigList().get(1)); // Remove construction heuristic.

        var solution = PlannerTestUtils.generateTestdataSolution("s1", 2);

        assertThatThrownBy(() -> PlannerTestUtils.solve(solverConfig, solution))
                .hasMessageContaining("uninitialized entities");
    }

    @Test
    void failsFastWithUninitializedSolutionListVariable() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataListSolutionExternalized.class, TestdataListEntityExternalized.class);
        solverConfig.withPhases(solverConfig.getPhaseConfigList().get(1)); // Remove construction heuristic.

        var solution = TestdataListSolutionExternalized.generateUninitializedSolution(6, 2);

        assertThatThrownBy(() -> PlannerTestUtils.solve(solverConfig, solution))
                .hasMessageContaining("planning list variable")
                .hasMessageContaining("unexpected unassigned values");
    }

}
