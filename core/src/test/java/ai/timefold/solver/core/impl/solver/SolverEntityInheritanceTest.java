package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testconstraint.DummyConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.classes.childnot.TestMultipleChildNotAnnotatedConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.classes.childnot.TestdataMultipleChildNotAnnotatedChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.classes.childnot.TestdataMultipleChildNotAnnotatedSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.classes.childtoo.TestMultipleBothAnnotatedConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.classes.childtoo.TestdataMultipleBothAnnotatedChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.classes.childtoo.TestdataMultipleBothAnnotatedSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.classes.mixed.TestMultipleMixedConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.classes.mixed.TestdataMultipleMixedChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.classes.mixed.TestdataMultipleMixedSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.interfaces.childnot.TestMultipleChildNotAnnotatedInterfaceConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.interfaces.childnot.TestdataMultipleChildNotAnnotatedInterfaceChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.interfaces.childnot.TestdataMultipleChildNotAnnotatedInterfaceSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.interfaces.childtoo.TestMultipleBothAnnotatedInterfaceConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.interfaces.childtoo.TestdataMultipleBothAnnotatedInterfaceChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.interfaces.childtoo.TestdataMultipleBothAnnotatedInterfaceSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.addvar.TestAddVarConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.addvar.TestdataAddVarChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.addvar.TestdataAddVarSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.childnot.TestChildNotAnnotatedConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.childnot.TestdataChildNotAnnotatedChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.childnot.TestdataChildNotAnnotatedSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.childtoo.TestBothAnnotatedConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.childtoo.TestdataBothAnnotatedChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.childtoo.TestdataBothAnnotatedSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.replacevar.TestdataReplaceVarChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.replacevar.TestdataReplaceVarSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.addvar.TestAddVarInterfaceConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.addvar.TestdataAddVarInterfaceChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.addvar.TestdataAddVarInterfaceSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.childnot.TestChildNotAnnotatedInterfaceConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.childnot.TestdataChildNotAnnotatedInterfaceChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.childnot.TestdataChildNotAnnotatedInterfaceSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.childtoo.TestBothAnnotatedInterfaceConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.childtoo.TestdataBothAnnotatedInterfaceChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.childtoo.TestdataBothAnnotatedInterfaceSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.replacevar.TestdataReplaceVarInterfaceChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.replacevar.TestdataReplaceVarInterfaceSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.basenot.classes.TestdataBaseNotAnnotatedChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.basenot.classes.TestdataBaseNotAnnotatedSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.basenot.interfaces.TestdataBaseNotAnnotatedInterfaceChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.basenot.interfaces.TestdataBaseNotAnnotatedInterfaceSolution;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class SolverEntityInheritanceTest {

    /**
     * This test validates the behavior of the solver
     * when both child and parent classes are annotated with {@code @PlanningEntity}.
     */
    @Test
    void testBothClassesAnnotated() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataBothAnnotatedSolution.class)
                .withEntityClasses(TestdataBothAnnotatedChildEntity.class)
                .withConstraintProviderClass(TestBothAnnotatedConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(20L));
        var problem = TestdataBothAnnotatedSolution.generateSolution(3, 2, false);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(2));
    }

    /**
     * This test validates the behavior of the solver
     * when both child and parent classes are annotated with {@code @PlanningEntity}
     * and the base entity is an interface.
     */
    @Test
    void testBothClassesAnnotatedAndBaseIsInterface() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataBothAnnotatedInterfaceSolution.class)
                .withEntityClasses(TestdataBothAnnotatedInterfaceChildEntity.class)
                .withConstraintProviderClass(TestBothAnnotatedInterfaceConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var problem = TestdataBothAnnotatedInterfaceSolution.generateSolution(3, 2, false);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(2));
    }

    /**
     * This test validates the behavior of the solver
     * when only the parent class is annotated with {@code @PlanningEntity}.
     */
    @Test
    void testOnlyBaseClassAnnotated() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataChildNotAnnotatedSolution.class)
                .withEntityClasses(TestdataChildNotAnnotatedChildEntity.class)
                .withConstraintProviderClass(TestChildNotAnnotatedConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var problem = TestdataChildNotAnnotatedSolution.generateSolution(3, 2, false);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(2));
    }

    /**
     * This test validates the behavior of the solver
     * when only the parent class is annotated with {@code @PlanningEntity}
     * and the base entity is an interface.
     */
    @Test
    void testOnlyBaseClassAnnotatedBaseIsInterface() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataChildNotAnnotatedInterfaceSolution.class)
                .withEntityClasses(TestdataChildNotAnnotatedInterfaceChildEntity.class)
                .withConstraintProviderClass(TestChildNotAnnotatedInterfaceConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var problem =
                TestdataChildNotAnnotatedInterfaceSolution
                        .generateSolution(3, 2, false);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(2));
    }

    /**
     * This test validates the behavior of the solver
     * when only the child class is annotated with {@code @PlanningEntity}.
     */
    @Test
    void testOnlyChildClassAnnotated() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataBaseNotAnnotatedSolution.class)
                .withEntityClasses(TestdataBaseNotAnnotatedChildEntity.class)
                .withConstraintProviderClass(DummyConstraintProvider.class);
        var solution = TestdataBaseNotAnnotatedSolution
                .generateSolution(3, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, solution))
                .hasMessageContaining(
                        "is not annotated with @PlanningEntity but defines genuine or shadow variables.");
    }

    /**
     * This test validates the behavior of the solver
     * when only the child class is annotated with {@code @PlanningEntity}
     * and the base entity is an interface.
     */
    @Test
    void testOnlyChildClassAnnotatedBaseIsInterface() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataBaseNotAnnotatedInterfaceSolution.class)
                .withEntityClasses(TestdataBaseNotAnnotatedInterfaceChildEntity.class)
                .withConstraintProviderClass(DummyConstraintProvider.class);
        var solution =
                TestdataBaseNotAnnotatedSolution
                        .generateSolution(3, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, solution))
                .hasMessageContaining(
                        "is not annotated with @PlanningEntity but defines genuine or shadow variables.");
    }

    /**
     * This test validates the behavior of the solver
     * when the annotated child class adds new variables.
     */
    @Test
    void testBothClassesAnnotatedAddNewVariable() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataAddVarSolution.class)
                .withEntityClasses(TestdataAddVarChildEntity.class)
                .withConstraintProviderClass(TestAddVarConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(100L));
        var problem = TestdataAddVarSolution.generateSolution(3, 2, false);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(4)); // value of 2 for each constraint
    }

    /**
     * This test validates the behavior of the solver
     * when the annotated child class adds new variables
     * and the base entity is an interface.
     */
    @Test
    void testBothClassesAnnotatedAddNewVariableBaseIsInterface() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataAddVarInterfaceSolution.class)
                .withEntityClasses(TestdataAddVarInterfaceChildEntity.class)
                .withConstraintProviderClass(TestAddVarInterfaceConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(100L));
        var problem = TestdataAddVarInterfaceSolution.generateSolution(3, 2, false);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(4)); // value of 2 for each constraint
    }

    /**
     * This test validates the behavior of the solver
     * when the annotated child class replaces an existing variable.
     */
    @Test
    void testBothClassesAnnotatedReplaceVariable() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataReplaceVarSolution.class)
                .withEntityClasses(TestdataReplaceVarChildEntity.class)
                .withConstraintProviderClass(DummyConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var solution = TestdataReplaceVarSolution.generateSolution(3, 2, false);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, solution))
                .hasMessageContaining("redefines the genuine variables ([value]), which is not permitted.");
    }

    /**
     * This test validates the behavior of the solver
     * when the annotated child class replaces an existing variable
     * and the base entity is an interface.
     */
    @Test
    void testBothClassesAnnotatedReplaceVariableBaseIsInterface() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataReplaceVarInterfaceSolution.class)
                .withEntityClasses(TestdataReplaceVarInterfaceChildEntity.class)
                .withConstraintProviderClass(DummyConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var solution = TestdataReplaceVarInterfaceSolution.generateSolution(3, 2, false);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, solution))
                .hasMessageContaining("redefines the genuine variables ([value]), which is not permitted.");
    }

    /**
     * This test validates the behavior of the solver
     * when both classes are annotated with {@code @PlanningEntity} and multiple inheritance is used.
     */
    @Test
    void testMultipleBothClassesAnnotated() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataMultipleBothAnnotatedSolution.class)
                .withEntityClasses(TestdataMultipleBothAnnotatedChildEntity.class)
                .withConstraintProviderClass(TestMultipleBothAnnotatedConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var problem =
                TestdataMultipleBothAnnotatedSolution
                        .generateSolution(3, 2, false);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining(
                        "Remove either the entity classes or entity interfaces from the inheritance chain to create a single-level inheritance structure");
    }

    /**
     * This test validates the behavior of the solver
     * when both classes are annotated with {@code @PlanningEntity}, multiple inheritance is used,
     * and the base entity is an interface.
     */
    @Test
    void testMultipleBothClassesAnnotatedBaseIsInterface() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataMultipleBothAnnotatedInterfaceSolution.class)
                .withEntityClasses(TestdataMultipleBothAnnotatedInterfaceChildEntity.class)
                .withConstraintProviderClass(TestMultipleBothAnnotatedInterfaceConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var problem = TestdataMultipleBothAnnotatedInterfaceSolution.generateSolution(3, 2, false);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining(
                        "Remove either the entity classes or entity interfaces from the inheritance chain to create a single-level inheritance structure");
    }

    /**
     * This test validates the behavior of the solver
     * when only the base class is annotated with {@code @PlanningEntity} and multiple inheritance is used.
     */
    @Test
    void testMultipleOnlyBaseClassAnnotated() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataMultipleChildNotAnnotatedSolution.class)
                .withEntityClasses(TestdataMultipleChildNotAnnotatedChildEntity.class)
                .withConstraintProviderClass(TestMultipleChildNotAnnotatedConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var problem = TestdataMultipleChildNotAnnotatedSolution.generateSolution(3, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining(
                        "Remove either the entity classes or entity interfaces from the inheritance chain to create a single-level inheritance structure");
    }

    /**
     * This test validates the behavior of the solver
     * when only the base class is annotated with {@code @PlanningEntity}, multiple inheritance is used,
     * and the base entity is an interface.
     */
    @Test
    void testMultipleOnlyBaseClassAnnotatedBaseIsInterface() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataMultipleChildNotAnnotatedInterfaceSolution.class)
                .withEntityClasses(TestdataMultipleChildNotAnnotatedInterfaceChildEntity.class)
                .withConstraintProviderClass(TestMultipleChildNotAnnotatedInterfaceConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var problem = TestdataMultipleChildNotAnnotatedInterfaceSolution.generateSolution(3, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining(
                        "Remove either the entity classes or entity interfaces from the inheritance chain to create a single-level inheritance structure");
    }

    /**
     * This test validates the behavior of the solver
     * when multiple inheritance is used, the child is annotated with {@code @PlanningEntity}
     * and it inherits from class and interface.
     */
    @Test
    void testMultipleBothClassesAnnotatedMixedPattern() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataMultipleMixedSolution.class)
                .withEntityClasses(TestdataMultipleMixedChildEntity.class)
                .withConstraintProviderClass(TestMultipleMixedConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var problem = TestdataMultipleMixedSolution.generateSolution(3, 2, false);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining("Mixed inheritance is not permitted.");// value of 2 for each constraint
    }
}
