package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testconstraint.DummyConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childnot.TestdataOnlyBaseAnnotatedChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childnot.TestdataOnlyBaseAnnotatedConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childnot.TestdataOnlyBaseAnnotatedExtendedSolution;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedExtendedConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedExtendedSolution;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtooabstract.TestdataBothAnnotatedAbstractChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtooabstract.TestdataBothAnnotatedAbstractExtendedConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtooabstract.TestdataBothAnnotatedAbstractExtendedSolution;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.multiple.TestdataMultipleInheritanceEntity;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.multiple.TestdataMultipleInheritanceExtendedSolution;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.replacemember.TestdataReplaceMemberEntity;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.replacemember.TestdataReplaceMemberExtendedSolution;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseanot.TestdataOnlyChildAnnotatedChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseanot.TestdataOnlyChildAnnotatedExtendedSolution;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

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
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(100L));

        var problem = TestdataBothAnnotatedExtendedSolution.generateSolution(3, 2);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(16));
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
                .withConstraintProviderClass(DummyConstraintProvider.class);

        var problem = TestdataBothAnnotatedExtendedSolution.generateSolution(3, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not annotated with @PlanningSolution but defines annotated members")
                .hasMessageContaining("Maybe annotate")
                .hasMessageContaining("with @PlanningSolution")
                .hasMessageContaining("Maybe remove the annotated members ([entityList, score, valueList])");
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

    /**
     * This test validates the behavior of the solver
     * when the child {@code @PlanningSolution} replaces an existing annotated member.
     */
    @Test
    void testReplaceAnnotatedMember() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataReplaceMemberExtendedSolution.class)
                .withEntityClasses(TestdataReplaceMemberEntity.class)
                .withConstraintProviderClass(DummyConstraintProvider.class);

        var problem = TestdataReplaceMemberExtendedSolution.generateSolution(3, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining("has a @PlanningEntityCollectionProperty annotated member")
                .hasMessageContaining("that is duplicated by a @PlanningEntityCollectionProperty annotated member")
                .hasMessageContaining("Maybe the annotation is defined on both the field and its getter.");
    }

    /**
     * This test validates the behavior of the solver
     * when multiple inheritance is applied.
     */
    @Test
    void testMultipleInheritance() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataMultipleInheritanceExtendedSolution.class)
                .withEntityClasses(TestdataMultipleInheritanceEntity.class)
                .withConstraintProviderClass(DummyConstraintProvider.class);

        var problem = TestdataReplaceMemberExtendedSolution.generateSolution(3, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining("inherits its @PlanningSolution annotation from multiple classes")
                .hasMessageContaining(
                        "Remove solution class(es) from the inheritance chain to create a single-level inheritance structure.");
    }
}
