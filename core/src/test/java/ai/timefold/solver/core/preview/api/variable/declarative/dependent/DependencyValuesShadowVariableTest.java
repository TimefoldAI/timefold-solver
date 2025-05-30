package ai.timefold.solver.core.preview.api.variable.declarative.dependent;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.move.streams.generic.move.ListAssignMove;
import ai.timefold.solver.core.impl.solver.MoveAsserter;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.testdomain.declarative.dependency.TestdataDependencyConstraintProvider;
import ai.timefold.solver.core.testdomain.declarative.dependency.TestdataDependencyEntity;
import ai.timefold.solver.core.testdomain.declarative.dependency.TestdataDependencySolution;
import ai.timefold.solver.core.testdomain.declarative.dependency.TestdataDependencyValue;

import org.junit.jupiter.api.Test;

class DependencyValuesShadowVariableTest {
    @Test
    void testSolve() {
        var baseTime = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
        var entityA = new TestdataDependencyEntity(baseTime);
        var entityB = new TestdataDependencyEntity(baseTime.plusMinutes(30));
        var entityC = new TestdataDependencyEntity(baseTime.plusMinutes(60));
        var entityD = new TestdataDependencyEntity(baseTime.plusMinutes(90));

        var valueA = new TestdataDependencyValue("A", Duration.ofMinutes(30));
        var valueB = new TestdataDependencyValue("B", Duration.ofMinutes(10));
        var valueC = new TestdataDependencyValue("C", Duration.ofMinutes(50));
        var valueD = new TestdataDependencyValue("D", Duration.ofMinutes(120));

        var valueA2 = new TestdataDependencyValue("(A,B)", Duration.ofMinutes(40), List.of(valueA, valueB));
        var valueB2 = new TestdataDependencyValue("(B,C)", Duration.ofMinutes(10), List.of(valueB, valueC));
        var valueC2 = new TestdataDependencyValue("(C,D)", Duration.ofMinutes(40), List.of(valueC, valueD));
        var valueD2 = new TestdataDependencyValue("(A,D)", Duration.ofMinutes(40), List.of(valueA, valueD));

        var valueA3 = new TestdataDependencyValue("(A,B)+C", Duration.ofMinutes(40), List.of(valueA2, valueC));
        var valueB3 = new TestdataDependencyValue("(B,C)+D", Duration.ofMinutes(10), List.of(valueB2, valueD));
        var valueC3 = new TestdataDependencyValue("(C,D)+A", Duration.ofMinutes(40), List.of(valueC2, valueA));
        var valueD3 = new TestdataDependencyValue("(A,D)+B", Duration.ofMinutes(40), List.of(valueD2, valueB));

        var schedule = new TestdataDependencySolution(List.of(entityA, entityB, entityC, entityD),
                List.of(valueA, valueB, valueC, valueD,
                        valueA2, valueB2, valueC2, valueD2,
                        valueA3, valueB3, valueC3, valueD3));

        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataDependencySolution.class)
                .withEntityClasses(TestdataDependencyEntity.class, TestdataDependencyValue.class)
                .withConstraintProviderClass(TestdataDependencyConstraintProvider.class)
                .withPreviewFeature(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withTerminationConfig(new TerminationConfig()
                        .withMoveCountLimit(1_000L));

        var solverFactory = SolverFactory.<TestdataDependencySolution> create(solverConfig);
        var solver = solverFactory.buildSolver();
        var solution = solver.solve(schedule);

        for (var value : solution.getValues()) {
            if (value.getDependencies() != null) {
                for (var dependency : value.getDependencies()) {
                    assertThat(value.getStartTime()).isAfterOrEqualTo(dependency.getEndTime());
                }
            }
        }
    }

    @Test
    void testLoopStatusOfEntityIsUpdatedEvenIfNoVariablesOnTheEntityChanged() {
        var baseTime = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
        var entityA = new TestdataDependencyEntity(baseTime);
        var entityB = new TestdataDependencyEntity(baseTime);
        var entityC = new TestdataDependencyEntity(baseTime);

        var valueA = new TestdataDependencyValue("A", Duration.ofHours(5), null);
        var valueB = new TestdataDependencyValue("B", Duration.ofHours(6), null);
        var valueC = new TestdataDependencyValue("C", Duration.ofHours(7), List.of(valueA, valueB));

        var schedule = new TestdataDependencySolution(
                List.of(entityA, entityB, entityC),
                List.of(valueA, valueB, valueC));

        entityA.getValues().add(valueB);
        entityA.getValues().add(valueA);

        var solutionDescriptor = SolutionDescriptor.buildSolutionDescriptor(Set.of(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES),
                TestdataDependencySolution.class, TestdataDependencyEntity.class, TestdataDependencyValue.class);
        var moveAsserter = MoveAsserter.create(solutionDescriptor);

        // Tests the move [A, B] -> [C, A, B].
        // Since C depends on A and B, this is an invalid solution,
        // and C.startTime/C.endTime remains null and C.isLooped is true.
        // When the move is undone, C.startTime/C.endTime remains null,
        // and C.isLooped is false.
        moveAsserter.assertMoveAndUndo(schedule, new ListAssignMove<>(
                (PlanningListVariableMetaModel<TestdataDependencySolution, ? super TestdataDependencyEntity, ? super TestdataDependencyValue>) solutionDescriptor
                        .getListVariableDescriptor().getVariableMetaModel(),
                valueC, entityA, 0));
    }
}
