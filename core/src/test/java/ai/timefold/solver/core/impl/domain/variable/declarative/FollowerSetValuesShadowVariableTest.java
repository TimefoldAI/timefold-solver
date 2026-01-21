package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.preview.api.move.MoveRunner;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.shadow.follower.TestdataFollowerConstraintProvider;
import ai.timefold.solver.core.testdomain.shadow.follower.TestdataLeaderEntity;
import ai.timefold.solver.core.testdomain.shadow.follower_set.TestdataFollowerSetEntity;
import ai.timefold.solver.core.testdomain.shadow.follower_set.TestdataFollowerSetSolution;

import org.junit.jupiter.api.Test;

class FollowerSetValuesShadowVariableTest {
    @Test
    void testSolve() {
        var problem = TestdataFollowerSetSolution.generateSolution(3, 8, 2);

        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataFollowerSetSolution.class)
                .withEntityClasses(TestdataLeaderEntity.class, TestdataFollowerSetEntity.class)
                .withConstraintProviderClass(TestdataFollowerConstraintProvider.class)
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

        var solutionMetamodel = TestdataFollowerSetSolution.buildSolutionMetaModel();
        var variableMetamodel =
                solutionMetamodel.entity(TestdataLeaderEntity.class).basicVariable("value", TestdataValue.class);
        var context = MoveRunner.build(solutionMetamodel)
                .using(solution);

        context.execute(Moves.change(variableMetamodel, leaderA, value1));
        assertThat(followerAB.getValue()).isEqualTo(value1);
        assertThat(followerAC.getValue()).isEqualTo(value1);
        assertThat(followerBC.getValue()).isNull();

        context.execute(Moves.change(variableMetamodel, leaderB, value2));
        assertThat(followerAB.getValue()).isEqualTo(value1);
        assertThat(followerAC.getValue()).isEqualTo(value1);
        assertThat(followerBC.getValue()).isEqualTo(value2);

        context.execute(Moves.change(variableMetamodel, leaderC, value1));
        assertThat(followerAB.getValue()).isEqualTo(value1);
        assertThat(followerAC.getValue()).isEqualTo(value1);
        assertThat(followerBC.getValue()).isEqualTo(value1);
    }
}
