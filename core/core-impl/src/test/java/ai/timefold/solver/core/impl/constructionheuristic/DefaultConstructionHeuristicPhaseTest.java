package ai.timefold.solver.core.impl.constructionheuristic;

import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCode;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.nullable.TestdataNullableEasyScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.nullable.TestdataNullableEntity;
import ai.timefold.solver.core.impl.testdata.domain.nullable.TestdataNullableSolution;
import ai.timefold.solver.core.impl.testdata.domain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.impl.testdata.domain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class DefaultConstructionHeuristicPhaseTest {

    @Test
    void solveWithInitializedEntities() {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        TestdataSolution solution = new TestdataSolution("s1");
        TestdataValue v1 = new TestdataValue("v1");
        TestdataValue v2 = new TestdataValue("v2");
        TestdataValue v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Arrays.asList(
                new TestdataEntity("e1", null),
                new TestdataEntity("e2", v2),
                new TestdataEntity("e3", v1)));

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
        TestdataEntity solvedE1 = solution.getEntityList().get(0);
        assertCode("e1", solvedE1);
        assertThat(solvedE1.getValue()).isNotNull();
        TestdataEntity solvedE2 = solution.getEntityList().get(1);
        assertCode("e2", solvedE2);
        assertThat(solvedE2.getValue()).isEqualTo(v2);
        TestdataEntity solvedE3 = solution.getEntityList().get(2);
        assertCode("e3", solvedE3);
        assertThat(solvedE3.getValue()).isEqualTo(v1);
        assertThat(solution.getScore().initScore()).isEqualTo(0);
    }

    @Test
    void solveWithInitializedSolution() {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        TestdataSolution inputProblem = new TestdataSolution("s1");
        TestdataValue v1 = new TestdataValue("v1");
        TestdataValue v2 = new TestdataValue("v2");
        TestdataValue v3 = new TestdataValue("v3");
        inputProblem.setValueList(Arrays.asList(v1, v2, v3));
        inputProblem.setEntityList(Arrays.asList(
                new TestdataEntity("e1", v1),
                new TestdataEntity("e2", v2),
                new TestdataEntity("e3", v3)));

        TestdataSolution solution = PlannerTestUtils.solve(solverConfig, inputProblem, false);
        assertThat(inputProblem).isSameAs(solution);
    }

    @Test
    void solveWithPinnedEntities() {
        SolverConfig solverConfig =
                PlannerTestUtils.buildSolverConfig(TestdataPinnedSolution.class, TestdataPinnedEntity.class)
                        .withPhases(new ConstructionHeuristicPhaseConfig());

        TestdataPinnedSolution solution = new TestdataPinnedSolution("s1");
        TestdataValue v1 = new TestdataValue("v1");
        TestdataValue v2 = new TestdataValue("v2");
        TestdataValue v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Arrays.asList(
                new TestdataPinnedEntity("e1", null, false, false),
                new TestdataPinnedEntity("e2", v2, true, false),
                new TestdataPinnedEntity("e3", null, false, true)));

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
        TestdataPinnedEntity solvedE1 = solution.getEntityList().get(0);
        assertCode("e1", solvedE1);
        assertThat(solvedE1.getValue()).isNotNull();
        TestdataPinnedEntity solvedE2 = solution.getEntityList().get(1);
        assertCode("e2", solvedE2);
        assertThat(solvedE2.getValue()).isEqualTo(v2);
        TestdataPinnedEntity solvedE3 = solution.getEntityList().get(2);
        assertCode("e3", solvedE3);
        assertThat(solvedE3.getValue()).isEqualTo(null);
        assertThat(solution.getScore().initScore()).isEqualTo(-1);
    }

    @Test
    void solveWithNullableEntities() {
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(TestdataNullableSolution.class)
                .withEntityClasses(TestdataNullableEntity.class)
                .withEasyScoreCalculatorClass(TestdataNullableEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        TestdataNullableSolution solution = new TestdataNullableSolution("s1");
        TestdataValue v1 = new TestdataValue("v1");
        TestdataValue v2 = new TestdataValue("v2");
        solution.setValueList(Arrays.asList(v1, v2));
        solution.setEntityList(Arrays.asList(
                new TestdataNullableEntity("e1", null),
                new TestdataNullableEntity("e2", null),
                new TestdataNullableEntity("e3", null)));

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
        TestdataNullableEntity solvedE1 = solution.getEntityList().get(0);
        assertCode("e1", solvedE1);
        assertThat(solvedE1.getValue()).isEqualTo(v1);
        TestdataNullableEntity solvedE2 = solution.getEntityList().get(1);
        assertCode("e2", solvedE2);
        assertThat(solvedE2.getValue()).isEqualTo(v2);
        TestdataNullableEntity solvedE3 = solution.getEntityList().get(2);
        assertCode("e3", solvedE3);
        assertThat(solvedE3.getValue()).isEqualTo(null);
        assertThat(solution.getScore().initScore()).isEqualTo(0);
        assertThat(solution.getScore().score()).isEqualTo(-1);
    }

    @Test
    void solveWithEmptyEntityList() {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        TestdataSolution solution = new TestdataSolution("s1");
        TestdataValue v1 = new TestdataValue("v1");
        TestdataValue v2 = new TestdataValue("v2");
        TestdataValue v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Collections.emptyList());

        solution = PlannerTestUtils.solve(solverConfig, solution, false);
        assertThat(solution).isNotNull();
        assertThat(solution.getEntityList()).isEmpty();
    }

}
