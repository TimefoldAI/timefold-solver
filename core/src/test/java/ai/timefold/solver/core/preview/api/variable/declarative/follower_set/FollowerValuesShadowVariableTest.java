package ai.timefold.solver.core.preview.api.variable.declarative.follower_set;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.move.streams.generic.move.ChangeMove;
import ai.timefold.solver.core.impl.solver.MoveAsserter;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.declarative.follower.TestdataFollowerConstraintProvider;
import ai.timefold.solver.core.testdomain.declarative.follower.TestdataLeaderEntity;
import ai.timefold.solver.core.testdomain.declarative.follower_set.TestdataFollowerSetEntity;
import ai.timefold.solver.core.testdomain.declarative.follower_set.TestdataFollowerSetSolution;

import org.junit.jupiter.api.Test;

class FollowerValuesShadowVariableTest {
    @Test
    void testSolve() {
        var problem = TestdataFollowerSetSolution.generateSolution(3, 8, 2);

        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataFollowerSetSolution.class)
                .withEntityClasses(TestdataLeaderEntity.class, TestdataFollowerSetEntity.class)
                .withConstraintProviderClass(TestdataFollowerConstraintProvider.class)
                .withPreviewFeature(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withTerminationConfig(new TerminationConfig()
                        .withMoveCountLimit(1_000L));

        var solverFactory = SolverFactory.<TestdataFollowerSetSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();
        var solution = solver.solve(problem);

        for (var follower : solution.getFollowers()) {
            assertThat(follower.getValue()).isEqualTo(follower.valueSupplier());
        }
    }

    @Test
    void testMove() {
        var leaderA = new TestdataLeaderEntity("A");
        var leaderB = new TestdataLeaderEntity("B");
        var leaderC = new TestdataLeaderEntity("C");

        var followerAB = new TestdataFollowerSetEntity("AB", List.of(leaderA, leaderB));
        var followerAC = new TestdataFollowerSetEntity("AC", List.of(leaderA, leaderC));
        var followerBC = new TestdataFollowerSetEntity("BC", List.of(leaderB, leaderC));

        var value1 = new TestdataValue("1");
        var value2 = new TestdataValue("2");

        var solution = new TestdataFollowerSetSolution("Solution",
                List.of(leaderA, leaderB, leaderC),
                List.of(followerAB, followerAC, followerBC),
                List.of(value1, value2));

        var solutionDescriptor = TestdataFollowerSetSolution.getSolutionDescriptor();
        var variableMetamodel = solutionDescriptor.getMetaModel().entity(TestdataLeaderEntity.class).variable("value");
        var moveAsserter = MoveAsserter.create(solutionDescriptor);

        moveAsserter.assertMoveAndApply(solution, new ChangeMove<>(
                (PlanningVariableMetaModel<TestdataFollowerSetSolution, ? super TestdataLeaderEntity, ? super TestdataValue>) variableMetamodel,
                leaderA, value1), newSolution -> {
                    assertThat(followerAB.getValue()).isEqualTo(value1);
                    assertThat(followerAC.getValue()).isEqualTo(value1);
                    assertThat(followerBC.getValue()).isEqualTo(null);
                });

        moveAsserter.assertMoveAndApply(solution, new ChangeMove<>(
                (PlanningVariableMetaModel<TestdataFollowerSetSolution, ? super TestdataLeaderEntity, ? super TestdataValue>) variableMetamodel,
                leaderB, value2), newSolution -> {
                    assertThat(followerAB.getValue()).isEqualTo(value1);
                    assertThat(followerAC.getValue()).isEqualTo(value1);
                    assertThat(followerBC.getValue()).isEqualTo(value2);
                });

        moveAsserter.assertMoveAndApply(solution, new ChangeMove<>(
                (PlanningVariableMetaModel<TestdataFollowerSetSolution, ? super TestdataLeaderEntity, ? super TestdataValue>) variableMetamodel,
                leaderC, value1), newSolution -> {
                    assertThat(followerAB.getValue()).isEqualTo(value1);
                    assertThat(followerAC.getValue()).isEqualTo(value1);
                    assertThat(followerBC.getValue()).isEqualTo(value1);
                });
    }
}
