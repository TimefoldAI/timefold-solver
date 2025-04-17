package ai.timefold.solver.core.preview.api.variable.declarative.concurrent_values;

import static ai.timefold.solver.core.impl.testdata.domain.declarative.concurrent_values.TestdataConcurrentValue.BASE_START_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.util.List;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.ChangedVariableNotifier;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultShadowVariableSessionFactory;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultTopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.declarative.VariableReferenceGraph;
import ai.timefold.solver.core.impl.testdata.domain.declarative.concurrent_values.TestdataConcurrentAssertionEasyScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.declarative.concurrent_values.TestdataConcurrentConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.declarative.concurrent_values.TestdataConcurrentEntity;
import ai.timefold.solver.core.impl.testdata.domain.declarative.concurrent_values.TestdataConcurrentSolution;
import ai.timefold.solver.core.impl.testdata.domain.declarative.concurrent_values.TestdataConcurrentValue;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConcurrentValuesShadowVariableTest {
    private record MockShadowVariableSession<Solution_>(SolutionDescriptor<Solution_> solutionDescriptor,
            VariableReferenceGraph<Solution_> graph) {

        public void setVariable(Object entity, String variableName, @Nullable Object value) {
            var variableMetamodel = solutionDescriptor.getMetaModel().entity(entity.getClass()).variable(variableName);
            graph.beforeVariableChanged(variableMetamodel, entity);
            solutionDescriptor.getEntityDescriptorStrict(entity.getClass()).getVariableDescriptor(variableName)
                    .setValue(entity, value);
            graph.afterVariableChanged(variableMetamodel, entity);
        }

        public void updateVariables() {
            graph.updateChanged();
        }
    }

    private <Solution_> MockShadowVariableSession<Solution_> createSession(SolutionDescriptor<Solution_> solutionDescriptor,
            Object... entities) {
        var variableReferenceGraph = new VariableReferenceGraph<Solution_>(ChangedVariableNotifier.empty());

        DefaultShadowVariableSessionFactory.visitGraph(solutionDescriptor, variableReferenceGraph, entities,
                DefaultTopologicalOrderGraph::new);

        return new MockShadowVariableSession<>(solutionDescriptor, variableReferenceGraph);
    }

    @Test
    void simpleChain() {
        var entity = spy(new TestdataConcurrentEntity("v1"));
        var value1 = spy(new TestdataConcurrentValue("c1"));
        var value2 = spy(new TestdataConcurrentValue("c2"));
        var value3 = spy(new TestdataConcurrentValue("c3"));

        var session = createSession(
                TestdataConcurrentSolution.buildSolutionDescriptor(),
                entity, value1, value2, value3);

        // First, test value1 -> value2 -> value3
        session.setVariable(value1, "entity", entity);
        session.setVariable(value2, "previousValue", value1);
        session.setVariable(value3, "previousValue", value2);

        Mockito.reset(entity, value1, value2, value3);
        session.updateVariables();

        assertStartsAfterDuration(Duration.ZERO, value1);
        assertStartsAfterDuration(Duration.ofMinutes(60L), value2);
        assertStartsAfterDuration(Duration.ofMinutes(120L), value3);

        // Second, test value1 -> value3 -> value2
        session.setVariable(value2, "previousValue", value3);
        session.setVariable(value3, "previousValue", value1);

        Mockito.reset(entity, value1, value2, value3);
        session.updateVariables();

        // value1 should have no interactions, since none of its dependencies are updated
        verifyNoInteractions(value1);

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

        var session = createSession(TestdataConcurrentSolution.buildSolutionDescriptor(),
                entity1, entity2, entity3, valueA1, valueA2, valueB1, valueB2, valueB3, valueC);

        // First test:
        // entity1: valueA1 -> valueB2
        // entity2: valueA2 -> valueB3
        // entity3: valueB1 -> valueC
        session.setVariable(valueA1, "entity", entity1);
        session.setVariable(valueA2, "entity", entity2);
        session.setVariable(valueB1, "entity", entity3);
        session.setVariable(valueB2, "previousValue", valueA1);
        session.setVariable(valueB3, "previousValue", valueA2);
        session.setVariable(valueC, "previousValue", valueB1);

        session.updateVariables();

        // No delay for A1/A2, since their entities arrive at the same time
        assertStartsAfterDuration(Duration.ZERO, valueA1, valueA2);

        // Delay B1 until the entities from A1/A2 are done (they are needed for values B2/B3)
        assertStartsAfterDuration(Duration.ofMinutes(60L), valueB1, valueB2, valueB3);
        assertStartsAfterDuration(Duration.ofMinutes(120L), valueC);

        // Second test:
        // entity1: valueC -> valueA1 -> valueB2
        // entity2: valueA2 -> valueB3
        // entity3: valueB1
        session.setVariable(valueC, "previousValue", null);
        session.setVariable(valueC, "entity", entity1);
        session.setVariable(valueA1, "previousValue", valueC);

        session.updateVariables();

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

        var session = createSession(TestdataConcurrentSolution.buildSolutionDescriptor(),
                entity1, entity2, entity3, valueA1, valueA2, valueB1, valueB2, valueB3, valueC);

        // First test:
        // entity1: valueA1 -> valueB2
        // entity2: valueA2 -> valueB3
        // entity3: valueB1 -> valueC
        session.setVariable(valueA1, "entity", entity1);
        session.setVariable(valueA2, "entity", entity2);
        session.setVariable(valueB1, "entity", entity3);
        session.setVariable(valueB2, "previousValue", valueA1);
        session.setVariable(valueB3, "previousValue", valueA2);
        session.setVariable(valueC, "previousValue", valueB1);

        session.updateVariables();

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
        session.setVariable(valueA1, "previousValue", valueB2);
        session.setVariable(valueB2, "entity", entity1);
        session.setVariable(valueB2, "previousValue", null);

        session.updateVariables();

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
        session.setVariable(valueC, "previousValue", null);
        session.setVariable(valueC, "entity", entity2);
        session.setVariable(valueA2, "previousValue", valueC);

        session.updateVariables();

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

        var session = createSession(TestdataConcurrentSolution.buildSolutionDescriptor(),
                entity1, entity2, entity3, valueA1, valueA2, valueB1, valueB2, valueB3, valueC);

        // First test:
        // entity1: valueB1 -> valueA1 -> valueB3
        // entity2: valueA2 -> valueB2
        // entity3: valueC
        // Loop between entity1 & entity2:
        // B1 is waiting for B2, which is waiting for A2
        // A2 is waiting for A1, which is waiting for B1
        session.setVariable(valueA1, "entity", entity1);
        session.setVariable(valueA2, "entity", entity2);
        session.setVariable(valueC, "entity", entity3);
        session.setVariable(valueB2, "previousValue", valueA1);
        session.setVariable(valueB3, "previousValue", valueA2);
        session.setVariable(valueA1, "previousValue", valueB1);

        session.updateVariables();

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
        session.setVariable(valueA1, "previousValue", null);
        session.setVariable(valueB1, "entity", entity3);
        session.setVariable(valueC, "previousValue", valueB1);

        session.updateVariables();

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
