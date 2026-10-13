package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testdomain.shadow.sibling.AbstractTestdataDeclarativeSiblingValue;
import ai.timefold.solver.core.testdomain.shadow.sibling.TestdataDeclarativeSiblingConstraintProvider;
import ai.timefold.solver.core.testdomain.shadow.sibling.TestdataDeclarativeSiblingEntity;
import ai.timefold.solver.core.testdomain.shadow.sibling.TestdataDeclarativeSiblingFirstValue;
import ai.timefold.solver.core.testdomain.shadow.sibling.TestdataDeclarativeSiblingSecondValue;
import ai.timefold.solver.core.testdomain.shadow.sibling.TestdataDeclarativeSiblingSolution;

import org.junit.jupiter.api.Test;

class DeclarativeShadowVariablesSiblingValuesTest {

    /**
     * Both siblings override the inherited source getter, so their source metamodels are equal
     * but carry different entity classes; changes to either sibling must reach the graph.
     */
    @Test
    void siblingValues() {
        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withSolutionClass(TestdataDeclarativeSiblingSolution.class)
                .withEntityClasses(TestdataDeclarativeSiblingEntity.class,
                        AbstractTestdataDeclarativeSiblingValue.class,
                        TestdataDeclarativeSiblingFirstValue.class,
                        TestdataDeclarativeSiblingSecondValue.class)
                .withConstraintProviderClass(TestdataDeclarativeSiblingConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig()
                        .withScoreCalculationCountLimit(1000L));
        var solver = SolverFactory.<TestdataDeclarativeSiblingSolution> create(solverConfig).buildSolver();

        var problem = new TestdataDeclarativeSiblingSolution();
        problem.setEntities(List.of(new TestdataDeclarativeSiblingEntity("A"), new TestdataDeclarativeSiblingEntity("B")));
        problem.setValues(List.of(new TestdataDeclarativeSiblingFirstValue("1"),
                new TestdataDeclarativeSiblingSecondValue("22"),
                new TestdataDeclarativeSiblingFirstValue("333"),
                new TestdataDeclarativeSiblingSecondValue("4444")));

        var solution = solver.solve(problem);

        for (var value : solution.getValues()) {
            var previousCode = value instanceof TestdataDeclarativeSiblingFirstValue firstValue
                    ? firstValue.getPreviousCode()
                    : ((TestdataDeclarativeSiblingSecondValue) value).getPreviousCode();
            assertThat(previousCode).isEqualTo(value.getPrevious() == null ? "" : value.getPrevious().getCode());
        }
    }

}
