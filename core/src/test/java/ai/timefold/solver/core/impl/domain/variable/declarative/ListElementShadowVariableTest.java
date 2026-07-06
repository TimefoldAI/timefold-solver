package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testdomain.shadow.list_element.TestdataListElementConstraintProvider;
import ai.timefold.solver.core.testdomain.shadow.list_element.TestdataListElementEntity;
import ai.timefold.solver.core.testdomain.shadow.list_element.TestdataListElementSolution;
import ai.timefold.solver.core.testdomain.shadow.list_element.TestdataListElementValue;

import org.junit.jupiter.api.Test;

/**
 * Tests declarative shadow variables sourced from the elements of a planning list variable,
 * i.e. {@code @ShadowSources("values[].endTime")} on the entity declaring the list variable.
 */
class ListElementShadowVariableTest {

    @Test
    void updateShadowVariables() {
        var valueA1 = new TestdataListElementValue("a1");
        var valueA2 = new TestdataListElementValue("a2");
        var valueB1 = new TestdataListElementValue("b1");
        var valueB2 = new TestdataListElementValue("b2");

        var entityA = new TestdataListElementEntity("A", 0);
        var entityB = new TestdataListElementEntity("B", 2);
        entityA.setValues(new ArrayList<>(List.of(valueA1, valueA2)));
        entityB.setValues(new ArrayList<>(List.of(valueB1, valueB2)));

        var solution = new TestdataListElementSolution();
        solution.setEntities(List.of(entityA, entityB));
        solution.setValues(List.of(valueA1, valueA2, valueB1, valueB2));

        SolutionManager.updateShadowVariables(solution);

        // A: start=0, a1 = [0, 1), a2 = [1, 2); B: start=2, b1 = [2, 3), b2 = [3, 4).
        assertThat(valueA1.getEndTime()).isEqualTo(1);
        assertThat(valueA2.getEndTime()).isEqualTo(2);
        assertThat(valueB1.getEndTime()).isEqualTo(3);
        assertThat(valueB2.getEndTime()).isEqualTo(4);

        // The aggregate shadow variable tracks the last element's endTime.
        assertThat(entityA.getLastEndTime()).isEqualTo(2);
        assertThat(entityB.getLastEndTime()).isEqualTo(4);
    }

    @Test
    void updateShadowVariablesEmptyList() {
        var entity = new TestdataListElementEntity("A", 3);
        var value = new TestdataListElementValue("v1");

        var solution = new TestdataListElementSolution();
        solution.setEntities(List.of(entity));
        solution.setValues(List.of(value));

        SolutionManager.updateShadowVariables(solution);

        // An entity with an empty list still gets its aggregate computed.
        assertThat(entity.getLastEndTime()).isEqualTo(3);
        assertThat(value.getEntity()).isNull();
        assertThat(value.getEndTime()).isNull();
    }

    @Test
    void updateShadowVariablesAfterChanges() {
        var value1 = new TestdataListElementValue("v1");
        var value2 = new TestdataListElementValue("v2");
        var value3 = new TestdataListElementValue("v3");

        var entityA = new TestdataListElementEntity("A", 0);
        var entityB = new TestdataListElementEntity("B", 10);
        entityA.setValues(new ArrayList<>(List.of(value1, value2, value3)));

        var solution = new TestdataListElementSolution();
        solution.setEntities(List.of(entityA, entityB));
        solution.setValues(List.of(value1, value2, value3));

        SolutionManager.updateShadowVariables(solution);
        assertThat(entityA.getLastEndTime()).isEqualTo(3);
        assertThat(entityB.getLastEndTime()).isEqualTo(10);

        // Move value3 from A to B and reverse A's remaining values.
        entityA.setValues(new ArrayList<>(List.of(value2, value1)));
        entityB.setValues(new ArrayList<>(List.of(value3)));

        SolutionManager.updateShadowVariables(solution);
        assertThat(entityA.getLastEndTime()).isEqualTo(2);
        assertThat(entityB.getLastEndTime()).isEqualTo(11);
    }

    @Test
    void solveWithFullAssertFromInitializedSolution() {
        // Starts from a non-empty assignment, so the initial fan-in edges
        // are seeded from the lists' contents when the graph is built.
        var problem = TestdataListElementSolution.generateSolution(2, 4);
        problem.getEntities().get(0).setValues(new ArrayList<>(problem.getValues().subList(0, 3)));
        problem.getEntities().get(1).setValues(new ArrayList<>(problem.getValues().subList(3, 4)));
        solveWithFullAssert(problem);
    }

    @Test
    void solveWithFullAssert() {
        var problem = TestdataListElementSolution.generateSolution(3, 6);
        solveWithFullAssert(problem);
    }

    private void solveWithFullAssert(TestdataListElementSolution problem) {

        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withSolutionClass(TestdataListElementSolution.class)
                .withEntityClasses(TestdataListElementEntity.class, TestdataListElementValue.class)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withConstraintProviderClass(TestdataListElementConstraintProvider.class))
                .withTerminationConfig(new TerminationConfig()
                        .withMoveCountLimit(1000L));

        var solverFactory = SolverFactory.<TestdataListElementSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();
        var solution = solver.solve(problem);

        assertThat(solution).isNotNull();
        for (var entity : solution.getEntities()) {
            var values = entity.getValues();
            var expectedLastEndTime = values.isEmpty() ? Integer.valueOf(entity.getStartTime())
                    : values.get(values.size() - 1).getEndTime();
            assertThat(entity.getLastEndTime()).isEqualTo(expectedLastEndTime);
        }
    }
}
