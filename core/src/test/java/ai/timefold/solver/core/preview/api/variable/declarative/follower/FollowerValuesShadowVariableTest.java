package ai.timefold.solver.core.preview.api.variable.declarative.follower;

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
import ai.timefold.solver.core.testdomain.declarative.follower.TestdataFollowerEntity;
import ai.timefold.solver.core.testdomain.declarative.follower.TestdataFollowerSolution;
import ai.timefold.solver.core.testdomain.declarative.follower.TestdataLeaderEntity;

import org.junit.jupiter.api.Test;

class FollowerValuesShadowVariableTest {
    @Test
    void testSolve() {
        var problem = TestdataFollowerSolution.generateSolution(3, 8, 2);

        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataFollowerSolution.class)
                .withEntityClasses(TestdataLeaderEntity.class, TestdataFollowerEntity.class)
                .withConstraintProviderClass(TestdataFollowerConstraintProvider.class)
                .withPreviewFeature(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withTerminationConfig(new TerminationConfig()
                        .withMoveCountLimit(1_000L));

        var solverFactory = SolverFactory.<TestdataFollowerSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();
        var solution = solver.solve(problem);

        for (var follower : solution.getFollowers()) {
            assertThat(follower.getValue()).isEqualTo(follower.getLeader().getValue());
        }
    }

    @Test
    void testMove() {
        var leaderA = new TestdataLeaderEntity("A");
        var leaderB = new TestdataLeaderEntity("B");
        var leaderC = new TestdataLeaderEntity("C");

        var followerA1 = new TestdataFollowerEntity("A1", leaderA);
        var followerA2 = new TestdataFollowerEntity("A2", leaderA);
        var followerA3 = new TestdataFollowerEntity("A3", leaderA);

        var followerB1 = new TestdataFollowerEntity("B1", leaderB);
        var followerB2 = new TestdataFollowerEntity("B2", leaderB);

        var value1 = new TestdataValue("1");
        var value2 = new TestdataValue("2");

        var solution = new TestdataFollowerSolution("Solution",
                List.of(leaderA, leaderB, leaderC),
                List.of(followerA1, followerA2, followerA3,
                        followerB1, followerB2),
                List.of(value1, value2));

        var solutionDescriptor = TestdataFollowerSolution.buildSolutionDescriptor();
        var variableMetamodel = solutionDescriptor.getMetaModel().entity(TestdataLeaderEntity.class).variable("value");
        var moveAsserter = MoveAsserter.create(solutionDescriptor);

        moveAsserter.assertMoveAndApply(solution, new ChangeMove<>(
                (PlanningVariableMetaModel<TestdataFollowerSolution, ? super TestdataLeaderEntity, ? super TestdataValue>) variableMetamodel,
                leaderA, value1), newSolution -> {
                    assertThat(followerA1.getValue()).isEqualTo(value1);
                    assertThat(followerA2.getValue()).isEqualTo(value1);
                    assertThat(followerA3.getValue()).isEqualTo(value1);

                    assertThat(followerB1.getValue()).isNull();
                    assertThat(followerB2.getValue()).isNull();
                });

        moveAsserter.assertMoveAndApply(solution, new ChangeMove<>(
                (PlanningVariableMetaModel<TestdataFollowerSolution, ? super TestdataLeaderEntity, ? super TestdataValue>) variableMetamodel,
                leaderB, value2), newSolution -> {
                    assertThat(followerA1.getValue()).isEqualTo(value1);
                    assertThat(followerA2.getValue()).isEqualTo(value1);
                    assertThat(followerA3.getValue()).isEqualTo(value1);

                    assertThat(followerB1.getValue()).isEqualTo(value2);
                    assertThat(followerB2.getValue()).isEqualTo(value2);
                });

        moveAsserter.assertMoveAndApply(solution, new ChangeMove<>(
                (PlanningVariableMetaModel<TestdataFollowerSolution, ? super TestdataLeaderEntity, ? super TestdataValue>) variableMetamodel,
                leaderC, value1), newSolution -> {
                    assertThat(followerA1.getValue()).isEqualTo(value1);
                    assertThat(followerA2.getValue()).isEqualTo(value1);
                    assertThat(followerA3.getValue()).isEqualTo(value1);

                    assertThat(followerB1.getValue()).isEqualTo(value2);
                    assertThat(followerB2.getValue()).isEqualTo(value2);
                });
    }
}
