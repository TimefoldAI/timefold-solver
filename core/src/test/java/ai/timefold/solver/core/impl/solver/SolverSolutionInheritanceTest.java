package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.testdata.constraints.DummyConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childnot.TestdataOnlyBaseAnnotatedChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childnot.TestdataOnlyBaseAnnotatedConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childnot.TestdataOnlyBaseAnnotatedExtendedSolution;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedExtendedConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedExtendedSolution;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtooabstract.TestdataBothAnnotatedAbstractChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtooabstract.TestdataBothAnnotatedAbstractExtendedConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtooabstract.TestdataBothAnnotatedAbstractExtendedSolution;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseanot.TestdataOnlyChildAnnotatedChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseanot.TestdataOnlyChildAnnotatedExtendedSolution;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class SolverSolutionInheritanceTest {

    /**
     * This test validates the behavior of the solver
     * when both child and parent solution classes are annotated with {@code @PlanningSolution}.
     */
    @Test
    void testBothClassesAnnotated() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataBothAnnotatedExtendedSolution.class)
                .withEntityClasses(TestdataBothAnnotatedChildEntity.class)
                .withConstraintProviderClass(TestdataBothAnnotatedExtendedConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(14L));

        var problem = TestdataBothAnnotatedExtendedSolution.generateSolution(3, 2);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(8));
    }

    /**
     * This test validates the behavior of the solver
     * when both child and parent solution classes are annotated with {@code @PlanningSolution},
     * and the base class is abstract.
     */
    @Test
    void testBothClassesAnnotatedBaseAbstract() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataBothAnnotatedAbstractExtendedSolution.class)
                .withEntityClasses(TestdataBothAnnotatedAbstractChildEntity.class)
                .withConstraintProviderClass(TestdataBothAnnotatedAbstractExtendedConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(14L));

        var problem = TestdataBothAnnotatedAbstractExtendedSolution.generateSolution(3, 2);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(8));
    }

    /**
     * This test validates the behavior of the solver
     * when only child class is annotated with {@code @PlanningSolution}.
     */
    @Test
    void testOnlyChildClassAnnotated() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataOnlyChildAnnotatedExtendedSolution.class)
                .withEntityClasses(TestdataOnlyChildAnnotatedChildEntity.class)
                .withConstraintProviderClass(DummyConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(14L));

        var problem = TestdataBothAnnotatedExtendedSolution.generateSolution(3, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maybe add a getScore() method with a @PlanningScore annotation.");
    }

    /**
     * This test validates the behavior of the solver
     * when only base class is annotated with {@code @PlanningSolution}.
     */
    @Test
    void testOnlyBaseClassAnnotated() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataOnlyBaseAnnotatedExtendedSolution.class)
                .withEntityClasses(TestdataOnlyBaseAnnotatedChildEntity.class)
                .withConstraintProviderClass(TestdataOnlyBaseAnnotatedConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(14L));

        var problem = TestdataOnlyBaseAnnotatedExtendedSolution.generateSolution(3, 2);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(2));
    }
}
