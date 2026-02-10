package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testdomain.shadow.always_looped.TestdataAlwaysLoopedConstraintProvider;
import ai.timefold.solver.core.testdomain.shadow.always_looped.TestdataAlwaysLoopedEntity;
import ai.timefold.solver.core.testdomain.shadow.always_looped.TestdataAlwaysLoopedSolution;

import org.junit.jupiter.api.Test;

class AlwaysLoopedShadowVariableTest {
    @Test
    void failsIfAlwaysLoopedVariablePresent() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataAlwaysLoopedSolution.class)
                .withEntityClasses(TestdataAlwaysLoopedEntity.class)
                .withConstraintProviderClass(TestdataAlwaysLoopedConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withBestScoreLimit("0"));
        var solverFactory = SolverFactory.create(solverConfig);
        var solver = solverFactory.buildSolver();
        var problem = new TestdataAlwaysLoopedSolution(
                List.of(new TestdataAlwaysLoopedEntity("e1"),
                        new TestdataAlwaysLoopedEntity("e2"),
                        new TestdataAlwaysLoopedEntity("e3")),
                List.of(1, 2, 3, 4, 5));
        assertThatCode(() -> solver.solve(problem)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(
                        "There are fixed dependency loops in the graph for variables",
                        "odd",
                        "even");
    }
}
