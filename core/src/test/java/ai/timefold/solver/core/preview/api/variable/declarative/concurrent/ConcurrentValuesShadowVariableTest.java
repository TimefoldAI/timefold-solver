package ai.timefold.solver.core.preview.api.variable.declarative.concurrent;

import static ai.timefold.solver.core.testdomain.declarative.concurrent.TestdataConcurrentValue.BASE_START_TIME;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testdomain.declarative.concurrent.TestdataConcurrentAssertionEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.declarative.concurrent.TestdataConcurrentConstraintProvider;
import ai.timefold.solver.core.testdomain.declarative.concurrent.TestdataConcurrentEntity;
import ai.timefold.solver.core.testdomain.declarative.concurrent.TestdataConcurrentSolution;
import ai.timefold.solver.core.testdomain.declarative.concurrent.TestdataConcurrentValue;

import org.junit.jupiter.api.Test;

class ConcurrentValuesShadowVariableTest {

    @Test
    void simpleChain() {
        var entity = new TestdataConcurrentEntity("v1");
        var value1 = new TestdataConcurrentValue("c1");
        var value2 = new TestdataConcurrentValue("c2");
        var value3 = new TestdataConcurrentValue("c3");
        entity.setValues(List.of(value1, value2, value3));
        SolutionManager.updateShadowVariables(TestdataConcurrentSolution.class, entity, value1, value2, value3);

        // First, test value1 -> value2 -> value3
        assertThat(value1.getPreviousValue()).isNull();
        assertThat(value1.getNextValue()).isSameAs(value2);
        assertThat(value1.getIndex()).isZero();
        assertThat(value1.getCascadingTime()).isEqualTo(BASE_START_TIME.plusDays(value1.getIndex()));
        assertThat(value2.getPreviousValue()).isSameAs(value1);
        assertThat(value2.getNextValue()).isSameAs(value3);
        assertThat(value2.getIndex()).isOne();
        assertThat(value2.getCascadingTime()).isEqualTo(BASE_START_TIME.plusDays(value2.getIndex()));
        assertThat(value3.getPreviousValue()).isSameAs(value2);
        assertThat(value3.getNextValue()).isNull();
        assertThat(value3.getIndex()).isEqualTo(2);
        assertThat(value3.getCascadingTime()).isEqualTo(BASE_START_TIME.plusDays(value3.getIndex()));
        assertStartsAfterDuration(Duration.ZERO, value1);
        assertStartsAfterDuration(Duration.ofMinutes(60L), value2);
        assertStartsAfterDuration(Duration.ofMinutes(120L), value3);

        // Second, test value1 -> value3 -> value2
        entity.setValues(List.of(value1, value3, value2));
        SolutionManager.updateShadowVariables(TestdataConcurrentSolution.class, entity, value1, value2, value3);
        assertThat(value1.getPreviousValue()).isNull();
        assertThat(value1.getNextValue()).isSameAs(value3);
        assertThat(value3.getPreviousValue()).isSameAs(value1);
        assertThat(value3.getNextValue()).isSameAs(value2);
        assertThat(value2.getPreviousValue()).isSameAs(value3);
        assertThat(value2.getNextValue()).isNull();
        assertStartsAfterDuration(Duration.ZERO, value1);
        assertStartsAfterDuration(Duration.ofMinutes(60), value3);
        assertStartsAfterDuration(Duration.ofMinutes(120), value2);
    }

    @Test
    void solutionSimpleChain() {
        var entity = new TestdataConcurrentEntity("v1");
        var value1 = new TestdataConcurrentValue("c1");
        var value2 = new TestdataConcurrentValue("c2");
        var value3 = new TestdataConcurrentValue("c3");
        entity.setValues(List.of(value1, value2, value3));
        var solution = new TestdataConcurrentSolution();
        solution.setEntities(List.of(entity));
        solution.setValues(List.of(value1, value2, value3));
        SolutionManager.updateShadowVariables(solution);

        // First, test value1 -> value2 -> value3
        assertThat(value1.getPreviousValue()).isNull();
        assertThat(value1.getNextValue()).isSameAs(value2);
        assertThat(value1.getIndex()).isZero();
        assertThat(value1.getCascadingTime()).isEqualTo(BASE_START_TIME.plusDays(value1.getIndex()));
        assertThat(value2.getPreviousValue()).isSameAs(value1);
        assertThat(value2.getNextValue()).isSameAs(value3);
        assertThat(value2.getIndex()).isOne();
        assertThat(value2.getCascadingTime()).isEqualTo(BASE_START_TIME.plusDays(value2.getIndex()));
        assertThat(value3.getPreviousValue()).isSameAs(value2);
        assertThat(value3.getNextValue()).isNull();
        assertThat(value3.getIndex()).isEqualTo(2);
        assertThat(value3.getCascadingTime()).isEqualTo(BASE_START_TIME.plusDays(value3.getIndex()));
        assertStartsAfterDuration(Duration.ZERO, value1);
        assertStartsAfterDuration(Duration.ofMinutes(60L), value2);
        assertStartsAfterDuration(Duration.ofMinutes(120L), value3);

        // Second, test value1 -> value3 -> value2
        entity.setValues(List.of(value1, value3, value2));
        SolutionManager.updateShadowVariables(solution);
        assertThat(value1.getPreviousValue()).isNull();
        assertThat(value1.getNextValue()).isSameAs(value3);
        assertThat(value3.getPreviousValue()).isSameAs(value1);
        assertThat(value3.getNextValue()).isSameAs(value2);
        assertThat(value2.getPreviousValue()).isSameAs(value3);
        assertThat(value2.getNextValue()).isNull();
        assertStartsAfterDuration(Duration.ZERO, value1);
        assertStartsAfterDuration(Duration.ofMinutes(60), value3);
        assertStartsAfterDuration(Duration.ofMinutes(120), value2);
    }

    @Test
    void groupChain() {
        var entity1 = new TestdataConcurrentEntity("v1");
        var entity2 = new TestdataConcurrentEntity("v2");
        var entity3 = new TestdataConcurrentEntity("v3");

        var valueA1 = new TestdataConcurrentValue("a1");
        var valueA2 = new TestdataConcurrentValue("a2");
        var valueB1 = new TestdataConcurrentValue("b1");
        var valueB2 = new TestdataConcurrentValue("b2");
        var valueB3 = new TestdataConcurrentValue("b3");
        var valueC = new TestdataConcurrentValue("c");

        var concurrentGroupA = List.of(valueA1, valueA2);
        var concurrentGroupB = List.of(valueB1, valueB2, valueB3);

        valueA1.setConcurrentValueGroup(concurrentGroupA);
        valueA2.setConcurrentValueGroup(concurrentGroupA);

        valueB1.setConcurrentValueGroup(concurrentGroupB);
        valueB2.setConcurrentValueGroup(concurrentGroupB);
        valueB3.setConcurrentValueGroup(concurrentGroupB);

        // First test:
        // entity1: valueA1 -> valueB2
        // entity2: valueA2 -> valueB3
        // entity3: valueB1 -> valueC
        entity1.setValues(List.of(valueA1, valueB2));
        entity2.setValues(List.of(valueA2, valueB3));
        entity3.setValues(List.of(valueB1, valueC));
        SolutionManager.updateShadowVariables(TestdataConcurrentSolution.class, entity1, entity2, entity3, valueA1, valueA2,
                valueB1, valueB2, valueB3, valueC);

        // No delay for A1/A2, since their entities arrive at the same time
        assertStartsAfterDuration(Duration.ZERO, valueA1, valueA2);

        // Delay B1 until the entities from A1/A2 are done (they are needed for values B2/B3)
        assertStartsAfterDuration(Duration.ofMinutes(60L), valueB1, valueB2, valueB3);
        assertStartsAfterDuration(Duration.ofMinutes(120L), valueC);

        // Second test:
        // entity1: valueC -> valueA1 -> valueB2
        // entity2: valueA2 -> valueB3
        // entity3: valueB1
        entity1.setValues(List.of(valueC, valueA1, valueB2));
        entity2.setValues(List.of(valueA2, valueB3));
        entity3.setValues(List.of(valueB1));
        SolutionManager.updateShadowVariables(TestdataConcurrentSolution.class, entity1, entity2, entity3, valueA1, valueA2,
                valueB1, valueB2, valueB3, valueC);

        // A1 is delayed because it is now after C
        // A2 is now delayed until A1 is ready
        assertStartsAfterDuration(Duration.ofMinutes(60L), valueA1, valueA2);

        assertStartsAfterDuration(Duration.ofMinutes(120L), valueB1, valueB2, valueB3);

        // Value C can now start immediately since it the first value and not in a concurrent group
        assertStartsAfterDuration(Duration.ZERO, valueC);
    }

    @Test
    void groupChainValidToInvalid() {
        var entity1 = new TestdataConcurrentEntity("v1");
        var entity2 = new TestdataConcurrentEntity("v2");
        var entity3 = new TestdataConcurrentEntity("v3");

        var valueA1 = new TestdataConcurrentValue("a1");
        var valueA2 = new TestdataConcurrentValue("a2");
        var valueB1 = new TestdataConcurrentValue("b1");
        var valueB2 = new TestdataConcurrentValue("b2");
        var valueB3 = new TestdataConcurrentValue("b3");
        var valueC = new TestdataConcurrentValue("c");

        var concurrentGroupA = List.of(valueA1, valueA2);
        var concurrentGroupB = List.of(valueB1, valueB2, valueB3);

        valueA1.setConcurrentValueGroup(concurrentGroupA);
        valueA2.setConcurrentValueGroup(concurrentGroupA);

        valueB1.setConcurrentValueGroup(concurrentGroupB);
        valueB2.setConcurrentValueGroup(concurrentGroupB);
        valueB3.setConcurrentValueGroup(concurrentGroupB);

        // First test:
        // entity1: valueA1 -> valueB2
        // entity2: valueA2 -> valueB3
        // entity3: valueB1 -> valueC
        entity1.setValues(List.of(valueA1, valueB2));
        entity2.setValues(List.of(valueA2, valueB3));
        entity3.setValues(List.of(valueB1, valueC));
        SolutionManager.updateShadowVariables(TestdataConcurrentSolution.class, entity1, entity2, entity3, valueA1, valueA2,
                valueB1, valueB2, valueB3, valueC);
        assertStartsAfterDuration(Duration.ZERO, valueA1, valueA2);
        assertStartsAfterDuration(Duration.ofMinutes(60L), valueB1, valueB2, valueB3);
        assertStartsAfterDuration(Duration.ofMinutes(120L), valueC);

        // Second test:
        // entity1: valueB2 -> valueA1
        // entity2: valueA2 -> valueB3
        // entity3: valueB1 -> valueC
        // Loop between entity1 & entity2:
        // B2 is waiting for B3, which is waiting for A2
        // A2 is waiting for A1, which is waiting for B2
        entity1.setValues(List.of(valueB2, valueA1));
        entity2.setValues(List.of(valueA2, valueB3));
        entity3.setValues(List.of(valueB1, valueC));
        SolutionManager.updateShadowVariables(TestdataConcurrentSolution.class, entity1, entity2, entity3, valueA1, valueA2,
                valueB1, valueB2, valueB3, valueC);

        // Everything is invalid/null, since no values are prior to the looped
        // groups.
        // C is invalid, since it is after the concurrent loop
        assertInvalid(valueA1, valueA2, valueB1, valueB2, valueB3, valueC);

        // Third test:
        // entity1: valueB2 -> valueA1
        // entity2: valueC -> valueA2 -> valueB3
        // entity3: valueB1
        // Loop between entity1 & entity2:
        // B2 is waiting for B3, which is waiting for A2
        // A2 is waiting for A1, which is waiting for B2
        entity1.setValues(List.of(valueB2, valueA1));
        entity2.setValues(List.of(valueC, valueA2, valueB3));
        entity3.setValues(List.of(valueB1));
        SolutionManager.updateShadowVariables(TestdataConcurrentSolution.class, entity1, entity2, entity3, valueA1, valueA2,
                valueB1, valueB2, valueB3, valueC);

        assertInvalid(valueA1, valueA2, valueB1, valueB2, valueB3);
        // C is valid, since it is prior to the concurrent loop
        assertStartsAfterDuration(Duration.ZERO, valueC);
    }

    @Test
    void groupChainInvalidToValid() {
        var entity1 = new TestdataConcurrentEntity("v1");
        var entity2 = new TestdataConcurrentEntity("v2");
        var entity3 = new TestdataConcurrentEntity("v3");

        var valueA1 = new TestdataConcurrentValue("a1");
        var valueA2 = new TestdataConcurrentValue("a2");
        var valueB1 = new TestdataConcurrentValue("b1");
        var valueB2 = new TestdataConcurrentValue("b2");
        var valueB3 = new TestdataConcurrentValue("b3");
        var valueC = new TestdataConcurrentValue("c");

        var concurrentGroupA = List.of(valueA1, valueA2);
        var concurrentGroupB = List.of(valueB1, valueB2, valueB3);

        valueA1.setConcurrentValueGroup(concurrentGroupA);
        valueA2.setConcurrentValueGroup(concurrentGroupA);

        valueB1.setConcurrentValueGroup(concurrentGroupB);
        valueB2.setConcurrentValueGroup(concurrentGroupB);
        valueB3.setConcurrentValueGroup(concurrentGroupB);

        // First test:
        // entity1: valueB1 -> valueA1 -> valueB3
        // entity2: valueA2 -> valueB2
        // entity3: valueC
        // Loop between entity1 & entity2:
        // B1 is waiting for B2, which is waiting for A2
        // A2 is waiting for A1, which is waiting for B1
        entity1.setValues(List.of(valueB1, valueA1, valueB3));
        entity2.setValues(List.of(valueA2, valueB2));
        entity3.setValues(List.of(valueC));
        SolutionManager.updateShadowVariables(TestdataConcurrentSolution.class, entity1, entity2, entity3, valueA1, valueA2,
                valueB1, valueB2, valueB3, valueC);
        // Everything except C is invalid
        assertInvalid(valueA1, valueA2, valueB1, valueB2, valueB3);
        // C is valid because it is not involved in or after a loop
        assertStartsAfterDuration(Duration.ZERO, valueC);

        // Second test:
        // entity1: valueA1
        // entity2: valueA2 -> valueB2
        // entity3: valueB1 -> valueC
        // Loop between entity1 & entity2:
        // B1 is waiting for B2, which is waiting for A2
        // A2 is waiting for A1, which is waiting for B1
        entity1.setValues(List.of(valueA1));
        entity2.setValues(List.of(valueA2, valueB2));
        entity3.setValues(List.of(valueB1, valueC));
        SolutionManager.updateShadowVariables(TestdataConcurrentSolution.class, entity1, entity2, entity3, valueA1, valueA2,
                valueB1, valueB2, valueB3, valueC);

        assertStartsAfterDuration(Duration.ZERO, valueA1, valueA2);
        assertStartsAfterDuration(Duration.ofMinutes(60), valueB1, valueB2, valueB3);
        assertStartsAfterDuration(Duration.ofMinutes(120), valueC);
    }

    private static void assertStartsAfterDuration(Duration duration, TestdataConcurrentValue... values) {
        for (var value : values) {
            assertThat(value.getServiceStartTime()).isEqualTo(BASE_START_TIME.plus(duration));
            assertThat(value.getServiceFinishTime()).isEqualTo(BASE_START_TIME.plus(duration).plusMinutes(30L));
            assertThat(value.isInvalid()).isFalse();
        }
    }

    private static void assertInvalid(TestdataConcurrentValue... values) {
        for (var value : values) {
            assertThat(value.getServiceStartTime()).isNull();
            assertThat(value.getServiceFinishTime()).isNull();
            assertThat(value.isInvalid()).isTrue();
        }
    }

    @Test
    void solveNoConcurrentValues() {
        var problem = new TestdataConcurrentSolution();
        var entity1 = new TestdataConcurrentEntity("v1");
        var entity2 = new TestdataConcurrentEntity("v2");
        var entity3 = new TestdataConcurrentEntity("v3");

        var value1 = new TestdataConcurrentValue("1");
        var value2 = new TestdataConcurrentValue("2");
        var value3 = new TestdataConcurrentValue("3");
        var value4 = new TestdataConcurrentValue("4");
        var value5 = new TestdataConcurrentValue("5");
        var value6 = new TestdataConcurrentValue("6");

        problem.setEntities(List.of(entity1, entity2, entity3));
        problem.setValues(List.of(value1, value2, value3, value4, value5, value6));

        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withPreviewFeature(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES)
                .withSolutionClass(TestdataConcurrentSolution.class)
                .withEntityClasses(TestdataConcurrentEntity.class, TestdataConcurrentValue.class)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withConstraintProviderClass(TestdataConcurrentConstraintProvider.class)
                        .withAssertionScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                                .withConstraintProviderClass(TestdataConcurrentConstraintProvider.class)))
                .withTerminationConfig(new TerminationConfig()
                        .withMoveCountLimit(1000L));

        var solverFactory = SolverFactory.<TestdataConcurrentSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();
        var solution = solver.solve(problem);

        assertThat(solution.getScore().isFeasible()).isTrue();
    }

    @Test
    void solveWithConcurrentValues() {
        var problem = new TestdataConcurrentSolution();
        var entity1 = new TestdataConcurrentEntity("v1");
        var entity2 = new TestdataConcurrentEntity("v2");
        var entity3 = new TestdataConcurrentEntity("v3");

        var valueA1 = new TestdataConcurrentValue("a1");
        var valueA2 = new TestdataConcurrentValue("a2");
        var valueB1 = new TestdataConcurrentValue("b1");
        var valueB2 = new TestdataConcurrentValue("b2");
        var valueB3 = new TestdataConcurrentValue("b3");
        var valueC = new TestdataConcurrentValue("c");

        var concurrentGroupA = List.of(valueA1, valueA2);
        var concurrentGroupB = List.of(valueB1, valueB2, valueB3);

        valueA1.setConcurrentValueGroup(concurrentGroupA);
        valueA2.setConcurrentValueGroup(concurrentGroupA);

        valueB1.setConcurrentValueGroup(concurrentGroupB);
        valueB2.setConcurrentValueGroup(concurrentGroupB);
        valueB3.setConcurrentValueGroup(concurrentGroupB);

        problem.setEntities(List.of(entity1, entity2, entity3));
        problem.setValues(List.of(valueA1, valueA2, valueB1, valueB2, valueB3, valueC));

        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withPreviewFeature(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES)
                .withSolutionClass(TestdataConcurrentSolution.class)
                .withEntityClasses(TestdataConcurrentEntity.class, TestdataConcurrentValue.class)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withConstraintProviderClass(TestdataConcurrentConstraintProvider.class)
                        .withAssertionScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                                .withEasyScoreCalculatorClass(TestdataConcurrentAssertionEasyScoreCalculator.class)))
                .withTerminationConfig(new TerminationConfig()
                        .withMoveCountLimit(1000L));

        var solverFactory = SolverFactory.<TestdataConcurrentSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();
        var solution = solver.solve(problem);

        assertThat(solution.getScore().isFeasible()).isTrue();
    }
}
