package ai.timefold.solver.spring.boot.autoconfigure;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.addvar.TestdataAddVarSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.childnot.TestdataChildNotAnnotatedSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.childtoo.TestdataBothAnnotatedSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.addvar.TestdataAddVarInterfaceSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.childnot.TestdataChildNotAnnotatedInterfaceSolution;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.childtoo.TestdataBothAnnotatedInterfaceSolution;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity.AddVarInterfaceSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity.AddVarSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity.BothAnnotatedInterfaceSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity.BothAnnotatedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity.MultipleBothAnnotatedInterfaceSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity.MultipleBothAnnotatedMixedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity.MultipleBothAnnotatedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity.MultipleOnlyBaseAnnotatedInterfaceSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity.MultipleOnlyBaseAnnotatedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity.OnlyBaseAnnotatedInterfaceSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity.OnlyBaseAnnotatedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity.OnlyChildAnnotatedInterfaceSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity.OnlyChildAnnotatedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity.ReplaceVarInterfaceSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.entity.ReplaceVarSpringTestConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.TestExecutionListeners;

@TestExecutionListeners
@Execution(ExecutionMode.CONCURRENT)
class TimefoldSolverEntityInheritanceTest {

    private final ApplicationContextRunner bothClassesAnnotatedContextRunner;
    private final ApplicationContextRunner bothClassesAnnotatedBaseInterfaceContextRunner;
    private final ApplicationContextRunner onlyBaseClassAnnotatedContextRunner;
    private final ApplicationContextRunner onlyBaseClassAnnotatedBaseInterfaceContextRunner;
    private final ApplicationContextRunner onlyChildClassAnnotatedContextRunner;
    private final ApplicationContextRunner onlyChildClassAnnotatedBaseInterfaceContextRunner;
    private final ApplicationContextRunner bothAnnotatedAddNewVarContextRunner;
    private final ApplicationContextRunner bothAnnotatedAddNewVarBaseInterfaceContextRunner;
    private final ApplicationContextRunner bothClassAnnotatedReplaceVarContextRunner;
    private final ApplicationContextRunner bothClassAnnotatedReplaceVarBaseInterfaceContextRunner;
    private final ApplicationContextRunner multipleBothClassesAnnotatedContextRunner;
    private final ApplicationContextRunner multipleBothClassesAnnotatedBaseInterfaceContextRunner;
    private final ApplicationContextRunner multipleBothClassesAnnotatedMixedPatternContextRunner;
    private final ApplicationContextRunner multipleOnlyBaseClassAnnotatedContextRunner;
    private final ApplicationContextRunner multipleOnlyBaseClassAnnotatedBaseInterfaceContextRunner;

    public TimefoldSolverEntityInheritanceTest() {
        bothClassesAnnotatedContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(BothAnnotatedSpringTestConfiguration.class);
        bothClassesAnnotatedBaseInterfaceContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(BothAnnotatedInterfaceSpringTestConfiguration.class);
        onlyBaseClassAnnotatedContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(OnlyBaseAnnotatedSpringTestConfiguration.class);
        onlyBaseClassAnnotatedBaseInterfaceContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(OnlyBaseAnnotatedInterfaceSpringTestConfiguration.class);
        onlyChildClassAnnotatedContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(OnlyChildAnnotatedSpringTestConfiguration.class);
        onlyChildClassAnnotatedBaseInterfaceContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(OnlyChildAnnotatedInterfaceSpringTestConfiguration.class);
        bothAnnotatedAddNewVarContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(AddVarSpringTestConfiguration.class);
        bothAnnotatedAddNewVarBaseInterfaceContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(AddVarInterfaceSpringTestConfiguration.class);
        bothClassAnnotatedReplaceVarContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(ReplaceVarSpringTestConfiguration.class);
        bothClassAnnotatedReplaceVarBaseInterfaceContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(ReplaceVarInterfaceSpringTestConfiguration.class);
        multipleBothClassesAnnotatedContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MultipleBothAnnotatedSpringTestConfiguration.class);
        multipleBothClassesAnnotatedBaseInterfaceContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MultipleBothAnnotatedInterfaceSpringTestConfiguration.class);
        multipleOnlyBaseClassAnnotatedContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MultipleOnlyBaseAnnotatedSpringTestConfiguration.class);
        multipleOnlyBaseClassAnnotatedBaseInterfaceContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MultipleOnlyBaseAnnotatedInterfaceSpringTestConfiguration.class);
        multipleBothClassesAnnotatedMixedPatternContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MultipleBothAnnotatedMixedSpringTestConfiguration.class);
    }

    /**
     * This test validates the behavior of the solver
     * when both child and parent classes are annotated with {@code @PlanningEntity}.
     */
    @Test
    void testBothClassesAnnotated() {
        assertThatCode(() -> bothClassesAnnotatedContextRunner
                .run(context -> {
                    var solverFactory = context.getBean(SolverFactory.class);
                    var problem = TestdataBothAnnotatedSolution.generateSolution(3, 2, false);
                    var solution = (TestdataBothAnnotatedSolution) solverFactory.buildSolver().solve(problem);
                    assertNotNull(solution);
                    assertThat(solution.getScore()).isEqualTo(SimpleScore.of(2));
                }))
                .doesNotThrowAnyException();
    }

    /**
     * This test validates the behavior of the solver
     * when both child and parent classes are annotated with {@code @PlanningEntity}
     * and the base entity is an interface.
     */
    @Test
    void testBothClassesAnnotatedAndBaseIsInterface() {
        assertThatCode(() -> bothClassesAnnotatedBaseInterfaceContextRunner
                .run(context -> {
                    var solverFactory = context.getBean(SolverFactory.class);
                    var problem = TestdataBothAnnotatedInterfaceSolution.generateSolution(3, 2, false);
                    var solution = (TestdataBothAnnotatedInterfaceSolution) solverFactory.buildSolver().solve(problem);
                    assertNotNull(solution);
                    assertThat(solution.getScore()).isEqualTo(SimpleScore.of(2));
                }))
                .doesNotThrowAnyException();
    }

    /**
     * This test validates the behavior of the solver
     * when only the parent class is annotated with {@code @PlanningEntity}.
     */
    @Test
    void testOnlyBaseClassAnnotated() {
        assertThatCode(() -> onlyBaseClassAnnotatedContextRunner
                .run(context -> {
                    var solverFactory = context.getBean(SolverFactory.class);
                    var problem = TestdataChildNotAnnotatedSolution.generateSolution(3, 2, false);
                    var solution = (TestdataChildNotAnnotatedSolution) solverFactory.buildSolver().solve(problem);
                    assertNotNull(solution);
                    assertThat(solution.getScore()).isEqualTo(SimpleScore.of(2));
                }))
                .doesNotThrowAnyException();
    }

    /**
     * This test validates the behavior of the solver
     * when only the parent class is annotated with {@code @PlanningEntity}
     * and the base entity is an interface.
     */
    @Test
    void testOnlyBaseClassAnnotatedBaseIsInterface() {
        assertThatCode(() -> onlyBaseClassAnnotatedBaseInterfaceContextRunner
                .run(context -> {
                    var solverFactory = context.getBean(SolverFactory.class);
                    var problem = TestdataChildNotAnnotatedInterfaceSolution.generateSolution(1, 2, false);
                    var solution = (TestdataChildNotAnnotatedInterfaceSolution) solverFactory.buildSolver().solve(problem);
                    assertNotNull(solution);
                    assertThat(solution.getScore()).isEqualTo(SimpleScore.of(2));
                }))
                .doesNotThrowAnyException();
    }

    /**
     * This test validates the behavior of the solver
     * when only the child class is annotated with {@code @PlanningEntity}.
     */
    @Test
    void testOnlyChildClassAnnotated() {
        assertThatCode(() -> onlyChildClassAnnotatedContextRunner
                .run(context -> context.getBean(SolverFactory.class)))
                .rootCause()
                .hasMessageContaining("is not annotated with @PlanningEntity but defines genuine or shadow variables.");
    }

    /**
     * This test validates the behavior of the solver
     * when only the child class is annotated with {@code @PlanningEntity}
     * and the base entity is an interface.
     */
    @Test
    void testOnlyChildClassAnnotatedBaseIsInterface() {
        assertThatCode(() -> onlyChildClassAnnotatedBaseInterfaceContextRunner
                .run(context -> context.getBean(SolverFactory.class)))
                .rootCause()
                .hasMessageContaining("is not annotated with @PlanningEntity but defines genuine or shadow variables.");
    }

    /**
     * This test validates the behavior of the solver
     * when the annotated child class adds new variables.
     */
    @Test
    void testBothClassesAnnotatedAddNewVariable() {
        assertThatCode(() -> bothAnnotatedAddNewVarContextRunner
                .run(context -> {
                    var solverFactory = context.getBean(SolverFactory.class);
                    var problem = TestdataAddVarSolution.generateSolution(3, 2, false);
                    var solution = (TestdataAddVarSolution) solverFactory.buildSolver().solve(problem);
                    assertNotNull(solution);
                    assertThat(solution.getScore()).isEqualTo(SimpleScore.of(4)); // value of 2 for each constraint
                }))
                .doesNotThrowAnyException();
    }

    /**
     * This test validates the behavior of the solver
     * when the annotated child class adds new variables
     * and the base entity is an interface.
     */
    @Test
    void testBothClassesAnnotatedAddNewVariableBaseIsInterface() {
        assertThatCode(() -> bothAnnotatedAddNewVarBaseInterfaceContextRunner
                .run(context -> {
                    var solverFactory = context.getBean(SolverFactory.class);
                    var problem =
                            TestdataAddVarInterfaceSolution.generateSolution(3, 2, false);
                    var solution = (TestdataAddVarInterfaceSolution) solverFactory.buildSolver().solve(problem);
                    assertNotNull(solution);
                    assertThat(solution.getScore()).isEqualTo(SimpleScore.of(4)); // value of 2 for each constraint
                }))
                .doesNotThrowAnyException();
    }

    /**
     * This test validates the behavior of the solver
     * when the annotated child class replaces an existing variable.
     */
    @Test
    void testBothClassesAnnotatedReplaceVariable() {
        assertThatCode(() -> bothClassAnnotatedReplaceVarContextRunner
                .run(context -> context.getBean(SolverFactory.class)))
                .rootCause()
                .hasMessageContaining("redefines the genuine variables ([value]), which is not permitted.");
    }

    /**
     * This test validates the behavior of the solver
     * when the annotated child class replaces an existing variable
     * and the base entity is an interface.
     */
    @Test
    void testBothClassesAnnotatedReplaceVariableBaseIsInterface() {
        assertThatCode(() -> bothClassAnnotatedReplaceVarBaseInterfaceContextRunner
                .run(context -> context.getBean(SolverFactory.class)))
                .rootCause()
                .hasMessageContaining("redefines the genuine variables ([value]), which is not permitted.");
    }

    /**
     * This test validates the behavior of the solver
     * when both classes are annotated with {@code @PlanningEntity} and multiple inheritance is used.
     */
    @Test
    void testMultipleBothClassesAnnotated() {
        assertThatCode(() -> multipleBothClassesAnnotatedContextRunner
                .run(context -> context.getBean(SolverFactory.class)))
                .rootCause()
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
        assertThatCode(() -> multipleBothClassesAnnotatedBaseInterfaceContextRunner
                .run(context -> context.getBean(SolverFactory.class)))
                .rootCause()
                .hasMessageContaining(
                        "Remove either the entity classes or entity interfaces from the inheritance chain to create a single-level inheritance structure");
    }

    /**
     * This test validates the behavior of the solver
     * when only the base class is annotated with {@code @PlanningEntity} and multiple inheritance is used.
     */
    @Test
    void testMultipleOnlyBaseClassAnnotated() {
        assertThatCode(() -> multipleOnlyBaseClassAnnotatedContextRunner
                .run(context -> context.getBean(SolverFactory.class)))
                .rootCause()
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
        assertThatCode(() -> multipleOnlyBaseClassAnnotatedBaseInterfaceContextRunner
                .run(context -> context.getBean(SolverFactory.class)))
                .rootCause()
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
        assertThatCode(() -> multipleBothClassesAnnotatedMixedPatternContextRunner
                .run(context -> context.getBean(SolverFactory.class)))
                .rootCause()
                .hasMessageContaining("Mixed inheritance is not permitted.");
    }
}
