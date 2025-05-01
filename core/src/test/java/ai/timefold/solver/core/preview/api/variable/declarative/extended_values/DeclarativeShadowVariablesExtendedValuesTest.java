package ai.timefold.solver.core.preview.api.variable.declarative.extended_values;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testdomain.declarative.extended.TestdataDeclarativeExtendedBaseValue;
import ai.timefold.solver.core.testdomain.declarative.extended.TestdataDeclarativeExtendedConstraintProvider;
import ai.timefold.solver.core.testdomain.declarative.extended.TestdataDeclarativeExtendedEntity;
import ai.timefold.solver.core.testdomain.declarative.extended.TestdataDeclarativeExtendedSolution;
import ai.timefold.solver.core.testdomain.declarative.extended.TestdataDeclarativeExtendedSubclassValue;

import org.junit.jupiter.api.Test;

class DeclarativeShadowVariablesExtendedValuesTest {

    @Test
    void extendedValues() {
        var solverConfig = new SolverConfig()
                .withPreviewFeature(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES)
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withSolutionClass(TestdataDeclarativeExtendedSolution.class)
                .withEntityClasses(TestdataDeclarativeExtendedEntity.class,
                        TestdataDeclarativeExtendedBaseValue.class,
                        TestdataDeclarativeExtendedSubclassValue.class)
                .withConstraintProviderClass(TestdataDeclarativeExtendedConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig()
                        .withScoreCalculationCountLimit(1000L));

        var solverFactory = SolverFactory.<TestdataDeclarativeExtendedSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();

        var problem = new TestdataDeclarativeExtendedSolution();
        var entityA = new TestdataDeclarativeExtendedEntity("A");
        var entityB = new TestdataDeclarativeExtendedEntity("B");

        var value1 = new TestdataDeclarativeExtendedSubclassValue("1");
        var value2 = new TestdataDeclarativeExtendedSubclassValue("2");
        var value31 = new TestdataDeclarativeExtendedSubclassValue("31");

        problem.setEntities(List.of(entityA, entityB));
        problem.setValues(List.of(value1, value2, value31));

        var solution = solver.solve(problem);

        assertThat(solution.getScore()).isEqualTo(HardSoftScore.of(0, 6));
    }

}
