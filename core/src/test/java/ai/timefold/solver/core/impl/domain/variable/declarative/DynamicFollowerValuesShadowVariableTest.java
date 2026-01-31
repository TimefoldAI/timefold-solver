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
import ai.timefold.solver.core.testdomain.shadow.dynamic_follower.TestdataDynamicFollowerConstraintProvider;
import ai.timefold.solver.core.testdomain.shadow.dynamic_follower.TestdataDynamicFollowerEntity;
import ai.timefold.solver.core.testdomain.shadow.dynamic_follower.TestdataDynamicFollowerSolution;
import ai.timefold.solver.core.testdomain.shadow.dynamic_follower.TestdataDynamicLeaderEntity;

import org.junit.jupiter.api.Test;

class DynamicFollowerValuesShadowVariableTest {
    @Test
    void testSolve() {
        var problem = TestdataDynamicFollowerSolution.generateSolution(3, 8, 2);

        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataDynamicFollowerSolution.class)
                .withEntityClasses(TestdataDynamicLeaderEntity.class, TestdataDynamicFollowerEntity.class)
                .withConstraintProviderClass(TestdataDynamicFollowerConstraintProvider.class)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withTerminationConfig(new TerminationConfig()
                        .withMoveCountLimit(1_000L));

        var solverFactory = SolverFactory.<TestdataDynamicFollowerSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();
        var solution = solver.solve(problem);

        for (var follower : solution.getFollowers()) {
            assertThat(follower.getValue()).isEqualTo(follower.getLeader().getValue());
        }
    }

    @Test
    void testLeaderValueChangeMove() {
        var leaderA = new TestdataDynamicLeaderEntity("Leader A");
        var leaderB = new TestdataDynamicLeaderEntity("Leader B");

        var follower1 = new TestdataDynamicFollowerEntity("Follower 1", leaderA);
        var follower2 = new TestdataDynamicFollowerEntity("Follower 2", leaderA);
        var follower3 = new TestdataDynamicFollowerEntity("Follower 3", leaderB);

        var value1 = new TestdataValue("1");
        var value2 = new TestdataValue("2");

        var solution = new TestdataDynamicFollowerSolution("Solution",
                List.of(leaderA, leaderB),
                List.of(follower1, follower2, follower3),
                List.of(value1, value2));

        var solutionMetamodel = TestdataDynamicFollowerSolution.buildMetaModel();
        var variableMetamodel = solutionMetamodel.genuineEntity(TestdataDynamicLeaderEntity.class)
                .basicVariable("value", TestdataValue.class);

        var context = MoveRunner.build(solutionMetamodel)
                .using(solution);
        context.execute(Moves.change(variableMetamodel, leaderA, value1));
        assertThat(follower1.getValue()).isEqualTo(value1);
        assertThat(follower2.getValue()).isEqualTo(value1);
        assertThat(follower3.getValue()).isNull();

        context.execute(Moves.change(variableMetamodel, leaderB, value2));
        assertThat(follower1.getValue()).isEqualTo(value1);
        assertThat(follower2.getValue()).isEqualTo(value1);
        assertThat(follower3.getValue()).isEqualTo(value2);

        context.execute(Moves.change(variableMetamodel, leaderA, value2));
        assertThat(follower1.getValue()).isEqualTo(value2);
        assertThat(follower2.getValue()).isEqualTo(value2);
        assertThat(follower3.getValue()).isEqualTo(value2);
    }

    @Test
    void testLeaderChangeMove() {
        var leaderA = new TestdataDynamicLeaderEntity("Leader A");
        var leaderB = new TestdataDynamicLeaderEntity("Leader B");

        var follower1 = new TestdataDynamicFollowerEntity("Follower 1", leaderA);
        var follower2 = new TestdataDynamicFollowerEntity("Follower 2", leaderA);
        var follower3 = new TestdataDynamicFollowerEntity("Follower 3", leaderB);

        var value1 = new TestdataValue("1");
        var value2 = new TestdataValue("2");

        leaderA.setValue(value1);
        leaderB.setValue(value2);

        follower1.setValue(value1);
        follower2.setValue(value1);
        follower3.setValue(value2);

        var solution = new TestdataDynamicFollowerSolution("Solution",
                List.of(leaderA, leaderB),
                List.of(follower1, follower2, follower3),
                List.of(value1, value2));

        var solutionMetamodel = TestdataDynamicFollowerSolution.buildMetaModel();
        var variableMetamodel = solutionMetamodel.genuineEntity(TestdataDynamicFollowerEntity.class)
                .basicVariable("leader", TestdataDynamicLeaderEntity.class);

        var context = MoveRunner.build(solutionMetamodel)
                .using(solution);

        context.execute(Moves.change(variableMetamodel, follower1, leaderB));
        assertThat(follower1.getValue()).isEqualTo(value2);
        assertThat(follower2.getValue()).isEqualTo(value1);
        assertThat(follower3.getValue()).isEqualTo(value2);

        context.execute(Moves.change(variableMetamodel, follower3, leaderA));
        assertThat(follower1.getValue()).isEqualTo(value2);
        assertThat(follower2.getValue()).isEqualTo(value1);
        assertThat(follower3.getValue()).isEqualTo(value1);

        context.execute(Moves.change(variableMetamodel, follower1, null));
        assertThat(follower1.getValue()).isNull();
        assertThat(follower2.getValue()).isEqualTo(value1);
        assertThat(follower3.getValue()).isEqualTo(value1);
    }

}
