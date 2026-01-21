package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testdomain.shadow.method_variables.TestdataDeclarativeMethodVariablesBaseValue;
import ai.timefold.solver.core.testdomain.shadow.method_variables.TestdataDeclarativeMethodVariablesConstraintProvider;
import ai.timefold.solver.core.testdomain.shadow.method_variables.TestdataDeclarativeMethodVariablesEntity;
import ai.timefold.solver.core.testdomain.shadow.method_variables.TestdataDeclarativeMethodVariablesSolution;
import ai.timefold.solver.core.testdomain.shadow.method_variables.TestdataDeclarativeMethodVariablesSubclassValue;

import org.junit.jupiter.api.Test;

class DeclarativeShadowVariablesMethodVariablesTest {

    @Test
    void methodDefinedVariables() {
        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withSolutionClass(TestdataDeclarativeMethodVariablesSolution.class)
                .withEntityClasses(TestdataDeclarativeMethodVariablesEntity.class,
                        TestdataDeclarativeMethodVariablesBaseValue.class,
                        TestdataDeclarativeMethodVariablesSubclassValue.class)
                .withConstraintProviderClass(TestdataDeclarativeMethodVariablesConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig()
                        .withScoreCalculationCountLimit(1000L));

        var solverFactory = SolverFactory.<TestdataDeclarativeMethodVariablesSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();

        var problem = new TestdataDeclarativeMethodVariablesSolution();
        var entityA = new TestdataDeclarativeMethodVariablesEntity("A");
        var entityB = new TestdataDeclarativeMethodVariablesEntity("B");

        var value1 = new TestdataDeclarativeMethodVariablesSubclassValue("1");
        var value2 = new TestdataDeclarativeMethodVariablesSubclassValue("2");
        var value31 = new TestdataDeclarativeMethodVariablesSubclassValue("31");

        problem.setEntities(List.of(entityA, entityB));
        problem.setValues(List.of(value1, value2, value31));

        var solution = solver.solve(problem);

        assertThat(solution.getScore()).isEqualTo(HardSoftScore.of(0, 6));
    }

}
