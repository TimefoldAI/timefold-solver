package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.testdata.constraints.DummyConstraintProvider;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class SolverInheritanceBasicVariableTest {

    /**
     * This test validates the behavior of the solver
     * when both child and parent classes are annotated with {@code @PlanningEntity}.
     */
    @Test
    void testBothClassesAnnotated() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotated.TestdataSolution.class)
                .withEntityClasses(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotated.TestdataChildEntity.class)
                .withConstraintProviderClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotated.TestConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var problem =
                ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotated.TestdataSolution
                        .generateSolution(3, 2, false);
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
                .withSolutionClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childannotated.TestdataSolution.class)
                .withEntityClasses(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childannotated.TestdataChildEntity.class)
                .withConstraintProviderClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childannotated.TestConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var problem =
                ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childannotated.TestdataSolution
                        .generateSolution(3, 2, false);
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
                .withSolutionClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childnotannotated.TestdataSolution.class)
                .withEntityClasses(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childnotannotated.TestdataChildEntity.class)
                .withConstraintProviderClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childnotannotated.TestConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var problem =
                ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childnotannotated.TestdataSolution
                        .generateSolution(3, 2, false);
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
                .withSolutionClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childnotannotated.TestdataSolution.class)
                .withEntityClasses(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childnotannotated.TestdataChildEntity.class)
                .withConstraintProviderClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childnotannotated.TestConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var problem =
                ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childnotannotated.TestdataSolution
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
                .withSolutionClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.basenotannotated.TestdataSolution.class)
                .withEntityClasses(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.basenotannotated.TestdataChildEntity.class)
                .withConstraintProviderClass(DummyConstraintProvider.class);
        var solution = ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.basenotannotated.TestdataSolution
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
                .withSolutionClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.basenotannotated.TestdataSolution.class)
                .withEntityClasses(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.basenotannotated.TestdataChildEntity.class)
                .withConstraintProviderClass(DummyConstraintProvider.class);
        var solution =
                ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.basenotannotated.TestdataSolution
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
                .withSolutionClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotatedaddvar.TestdataSolution.class)
                .withEntityClasses(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotatedaddvar.TestdataChildEntity.class)
                .withConstraintProviderClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotatedaddvar.TestConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(100L));
        var problem =
                ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotatedaddvar.TestdataSolution
                        .generateSolution(3, 2, false);
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
                .withSolutionClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childannotatedaddvar.TestdataSolution.class)
                .withEntityClasses(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childannotatedaddvar.TestdataChildEntity.class)
                .withConstraintProviderClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childannotatedaddvar.TestConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(100L));
        var problem =
                ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childannotatedaddvar.TestdataSolution
                        .generateSolution(3, 2, false);
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
                .withSolutionClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotatedreplacevar.TestdataSolution.class)
                .withEntityClasses(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotatedreplacevar.TestdataChildEntity.class)
                .withConstraintProviderClass(DummyConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var solution =
                ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotatedreplacevar.TestdataSolution
                        .generateSolution(3, 2, false);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, solution))
                .hasMessageContaining("redefines the genuine variables [value], which is not permitted.");
    }

    /**
     * This test validates the behavior of the solver
     * when the annotated child class replaces an existing variable
     * and the base entity is an interface.
     */
    @Test
    void testBothClassesAnnotatedReplaceVariableBaseIsInterface() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childannotatedreplacevar.TestdataSolution.class)
                .withEntityClasses(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childannotatedreplacevar.TestdataChildEntity.class)
                .withConstraintProviderClass(DummyConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var solution =
                ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childannotatedreplacevar.TestdataSolution
                        .generateSolution(3, 2, false);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, solution))
                .hasMessageContaining("redefines the genuine variables [value], which is not permitted.");
    }

    /**
     * This test validates the behavior of the solver
     * when both classes are annotated with {@code @PlanningEntity} and multiple inheritance is used.
     */
    @Test
    void testMultipleBothClassesAnnotated() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childannotated.TestdataMultipleSolution.class)
                .withEntityClasses(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childannotated.TestdataMultipleChildEntity.class)
                .withConstraintProviderClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childannotated.TestMultipleConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var problem =
                ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childannotated.TestdataMultipleSolution
                        .generateSolution(3, 2, false);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining("The multiple inheritance is not allowed.");
    }

    /**
     * This test validates the behavior of the solver
     * when both classes are annotated with {@code @PlanningEntity}, multiple inheritance is used,
     * and the base entity is an interface.
     */
    @Test
    void testMultipleBothClassesAnnotatedBaseIsInterface() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritanceinterface.baseannotated.childannotated.TestdataMultipleSolution.class)
                .withEntityClasses(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritanceinterface.baseannotated.childannotated.TestdataMultipleChildEntity.class)
                .withConstraintProviderClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritanceinterface.baseannotated.childannotated.TestMultipleConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var problem =
                ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritanceinterface.baseannotated.childannotated.TestdataMultipleSolution
                        .generateSolution(3, 2, false);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining("The multiple inheritance is not allowed.");
    }

    /**
     * This test validates the behavior of the solver
     * when only the base class is annotated with {@code @PlanningEntity} and multiple inheritance is used.
     */
    @Test
    void testMultipleOnlyBaseClassAnnotated() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childnotannotated.TestdataMultipleSolution.class)
                .withEntityClasses(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childnotannotated.TestdataMultipleChildEntity.class)
                .withConstraintProviderClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childnotannotated.TestMultipleConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var problem =
                ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childnotannotated.TestdataMultipleSolution
                        .generateSolution(3, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining("The multiple inheritance is not allowed.");
    }

    /**
     * This test validates the behavior of the solver
     * when only the base class is annotated with {@code @PlanningEntity}, multiple inheritance is used,
     * and the base entity is an interface.
     */
    @Test
    void testMultipleOnlyBaseClassAnnotatedBaseIsInterface() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritanceinterface.baseannotated.childnotannotated.TestdataMultipleSolution.class)
                .withEntityClasses(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritanceinterface.baseannotated.childnotannotated.TestdataMultipleChildEntity.class)
                .withConstraintProviderClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritanceinterface.baseannotated.childnotannotated.TestMultipleConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var problem =
                ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritanceinterface.baseannotated.childnotannotated.TestdataMultipleSolution
                        .generateSolution(3, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining("The multiple inheritance is not allowed.");
    }

    /**
     * This test validates the behavior of the solver
     * when multiple inheritance is used, the child is annotated with {@code @PlanningEntity}
     * and it inherits from class and interface.
     */
    @Test
    void testMultipleBothClassesAnnotatedMixedPattern() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.mixed.childannotated.TestdataMultipleSolution.class)
                .withEntityClasses(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.mixed.childannotated.TestdataMultipleChildEntity.class)
                .withConstraintProviderClass(
                        ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.mixed.childannotated.TestMultipleConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L));
        var problem =
                ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.mixed.childannotated.TestdataMultipleSolution
                        .generateSolution(3, 2, false);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining("Mixed inheritance is not permitted.");// value of 2 for each constraint
    }
}
