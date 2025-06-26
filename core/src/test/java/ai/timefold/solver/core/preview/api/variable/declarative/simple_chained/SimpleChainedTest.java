package ai.timefold.solver.core.preview.api.variable.declarative.simple_chained;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testdomain.declarative.simple_chained.TestdataChainedSimpleVarConstraintProvider;
import ai.timefold.solver.core.testdomain.declarative.simple_chained.TestdataChainedSimpleVarEntity;
import ai.timefold.solver.core.testdomain.declarative.simple_chained.TestdataChainedSimpleVarSolution;
import ai.timefold.solver.core.testdomain.declarative.simple_chained.TestdataChainedSimpleVarValue;

import org.junit.jupiter.api.Test;

class SimpleChainedTest {
    @Test
    void simpleChained() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataChainedSimpleVarSolution.class)
                .withEntityClasses(TestdataChainedSimpleVarEntity.class, TestdataChainedSimpleVarValue.class)
                .withConstraintProviderClass(TestdataChainedSimpleVarConstraintProvider.class)
                .withPreviewFeature(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withTerminationConfig(new TerminationConfig().withBestScoreLimit("-48"));

        var entityList = List.of(new TestdataChainedSimpleVarEntity("e1", Duration.ofDays(1)),
                new TestdataChainedSimpleVarEntity("e2", Duration.ofDays(2)),
                new TestdataChainedSimpleVarEntity("e3", Duration.ofDays(3)));
        var valueList = List.of(
                new TestdataChainedSimpleVarValue("a1", Duration.ofDays(1)),
                new TestdataChainedSimpleVarValue("a2", Duration.ofDays(2)),
                new TestdataChainedSimpleVarValue("a3", Duration.ofDays(3)));
        var problem = new TestdataChainedSimpleVarSolution(entityList, valueList);
        var solverFactory = SolverFactory.<TestdataChainedSimpleVarSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();
        var solution = solver.solve(problem);

        // In the optimal solution, each value is paired with the opposite entity
        // i.e. v1 -> e3, v2 -> e2, and v3 -> e1.
        var values = solution.getValues();
        for (var value : values) {
            assertThat(value.getCumulativeDurationInDays()).isEqualTo(4);
        }
    }
}
