package ai.timefold.solver.spring.boot.autoconfigure;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.spring.boot.autoconfigure.superclass.BothClassesAnnotatedAddNewVarBaseInterfaceSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.superclass.BothClassesAnnotatedAddNewVarSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.superclass.BothClassesAnnotatedBaseInterfaceSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.superclass.BothClassesAnnotatedReplaceVarBaseInterfaceSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.superclass.BothClassesAnnotatedReplaceVarSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.superclass.BothClassesAnnotatedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.superclass.MultipleBothClassesAnnotatedBaseInterfaceSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.superclass.MultipleBothClassesAnnotatedMixedPatternSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.superclass.MultipleBothClassesAnnotatedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.superclass.MultipleOnlyBaseClassAnnotatedBaseInterfaceSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.superclass.MultipleOnlyBaseClassAnnotatedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.superclass.OnlyBaseClassAnnotatedBaseInterfaceSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.superclass.OnlyBaseClassAnnotatedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.superclass.OnlyChildClassAnnotatedBaseInterfaceSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.superclass.OnlyChildClassAnnotatedSpringTestConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.TestExecutionListeners;

@TestExecutionListeners
class TimefoldSolverInheritanceTest {

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

    public TimefoldSolverInheritanceTest() {
        bothClassesAnnotatedContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(BothClassesAnnotatedSpringTestConfiguration.class);
        bothClassesAnnotatedBaseInterfaceContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(BothClassesAnnotatedBaseInterfaceSpringTestConfiguration.class);
        onlyBaseClassAnnotatedContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(OnlyBaseClassAnnotatedSpringTestConfiguration.class);
        onlyBaseClassAnnotatedBaseInterfaceContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(OnlyBaseClassAnnotatedBaseInterfaceSpringTestConfiguration.class);
        onlyChildClassAnnotatedContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(OnlyChildClassAnnotatedSpringTestConfiguration.class);
        onlyChildClassAnnotatedBaseInterfaceContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(OnlyChildClassAnnotatedBaseInterfaceSpringTestConfiguration.class);
        bothAnnotatedAddNewVarContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(BothClassesAnnotatedAddNewVarSpringTestConfiguration.class);
        bothAnnotatedAddNewVarBaseInterfaceContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(BothClassesAnnotatedAddNewVarBaseInterfaceSpringTestConfiguration.class);
        bothClassAnnotatedReplaceVarContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(BothClassesAnnotatedReplaceVarSpringTestConfiguration.class);
        bothClassAnnotatedReplaceVarBaseInterfaceContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(BothClassesAnnotatedReplaceVarBaseInterfaceSpringTestConfiguration.class);
        multipleBothClassesAnnotatedContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MultipleBothClassesAnnotatedSpringTestConfiguration.class);
        multipleBothClassesAnnotatedBaseInterfaceContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MultipleBothClassesAnnotatedBaseInterfaceSpringTestConfiguration.class);
        multipleOnlyBaseClassAnnotatedContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MultipleOnlyBaseClassAnnotatedSpringTestConfiguration.class);
        multipleOnlyBaseClassAnnotatedBaseInterfaceContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MultipleOnlyBaseClassAnnotatedBaseInterfaceSpringTestConfiguration.class);
        multipleBothClassesAnnotatedMixedPatternContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MultipleBothClassesAnnotatedMixedPatternSpringTestConfiguration.class);
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
                    var problem =
                            ai.timefold.solver.core.impl.testdata.domain.superclass.singlevar.baseannotated.childannotated.TestdataSolution
                                    .generateSolution(1, 1);
                    var solution =
                            (ai.timefold.solver.core.impl.testdata.domain.superclass.singlevar.baseannotated.childannotated.TestdataSolution) solverFactory
                                    .buildSolver().solve(problem);
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
                    var problem =
                            ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.baseannotated.childannotated.TestdataSolution
                                    .generateSolution(1, 1);
                    var solution =
                            (ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.baseannotated.childannotated.TestdataSolution) solverFactory
                                    .buildSolver().solve(problem);
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
                    var problem =
                            ai.timefold.solver.core.impl.testdata.domain.superclass.singlevar.baseannotated.childnotannotated.TestdataSolution
                                    .generateSolution(1, 2);
                    var solution =
                            (ai.timefold.solver.core.impl.testdata.domain.superclass.singlevar.baseannotated.childnotannotated.TestdataSolution) solverFactory
                                    .buildSolver().solve(problem);
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
                    var problem =
                            ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.baseannotated.childnotannotated.TestdataSolution
                                    .generateSolution(1, 2);
                    var solution =
                            (ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.baseannotated.childnotannotated.TestdataSolution) solverFactory
                                    .buildSolver().solve(problem);
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
                .run(context -> context.getBean(SolverConfig.class)))
                .rootCause().hasMessageContaining(
                        "The classes ([TestdataBaseEntity]) do not have the PlanningEntity annotation, even though they contain properties reserved for planning entities.");
    }

    /**
     * This test validates the behavior of the solver
     * when only the child class is annotated with {@code @PlanningEntity}
     * and the base entity is an interface.
     */
    @Test
    void testOnlyChildClassAnnotatedBaseIsInterface() {
        assertThatCode(() -> onlyChildClassAnnotatedBaseInterfaceContextRunner
                .run(context -> context.getBean(SolverConfig.class)))
                .rootCause().hasMessageContaining(
                        "The classes ([TestdataBaseEntity]) do not have the PlanningEntity annotation, even though they contain properties reserved for planning entities.");
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
                    var problem =
                            ai.timefold.solver.core.impl.testdata.domain.superclass.singlevar.baseannotated.childannotatedaddvar.TestdataSolution
                                    .generateSolution(1, 2, false);
                    var solution =
                            (ai.timefold.solver.core.impl.testdata.domain.superclass.singlevar.baseannotated.childannotatedaddvar.TestdataSolution) solverFactory
                                    .buildSolver().solve(problem);
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
                            ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.baseannotated.childannotatedaddvar.TestdataSolution
                                    .generateSolution(1, 2, false);
                    var solution =
                            (ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.baseannotated.childannotatedaddvar.TestdataSolution) solverFactory
                                    .buildSolver().solve(problem);
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
                .run(context -> {
                    var solverFactory = context.getBean(SolverFactory.class);
                    var problem =
                            ai.timefold.solver.core.impl.testdata.domain.superclass.singlevar.baseannotated.childannotatedreplacevar.TestdataSolution
                                    .generateSolution(1, 2, false);
                    var solution =
                            (ai.timefold.solver.core.impl.testdata.domain.superclass.singlevar.baseannotated.childannotatedreplacevar.TestdataSolution) solverFactory
                                    .buildSolver().solve(problem);
                    assertNotNull(solution);
                }))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * This test validates the behavior of the solver
     * when the annotated child class replaces an existing variable
     * and the base entity is an interface.
     */
    @Test
    void testBothClassesAnnotatedReplaceVariableBaseIsInterface() {
        assertThatCode(() -> bothClassAnnotatedReplaceVarBaseInterfaceContextRunner
                .run(context -> {
                    var solverFactory = context.getBean(SolverFactory.class);
                    var problem =
                            ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.baseannotated.childannotatedreplacevar.TestdataSolution
                                    .generateSolution(1, 2, false);
                    var solution =
                            (ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.baseannotated.childannotatedreplacevar.TestdataSolution) solverFactory
                                    .buildSolver().solve(problem);
                    assertNotNull(solution);
                }))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * This test validates the behavior of the solver
     * when both classes are annotated with {@code @PlanningEntity} and multiple inheritance is used.
     */
    @Test
    void testMultipleBothClassesAnnotated() {
        assertThatCode(() -> multipleBothClassesAnnotatedContextRunner
                .run(context -> {
                    var solverFactory = context.getBean(SolverFactory.class);
                    var problem =
                            ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritance.baseannotated.childannotated.TestdataMultipleSolution
                                    .generateSolution(1, 2, false);
                    var solution =
                            (ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritance.baseannotated.childannotated.TestdataMultipleSolution) solverFactory
                                    .buildSolver().solve(problem);
                    assertNotNull(solution);
                    assertThat(solution.getScore()).isEqualTo(SimpleScore.of(4)); // value of 2 for each constraint
                }))
                .doesNotThrowAnyException();
    }

    /**
     * This test validates the behavior of the solver
     * when both classes are annotated with {@code @PlanningEntity}, multiple inheritance is used,
     * and the base entity is an interface.
     */
    @Test
    void testMultipleBothClassesAnnotatedBaseIsInterface() {
        assertThatCode(() -> multipleBothClassesAnnotatedBaseInterfaceContextRunner
                .run(context -> {
                    var solverFactory = context.getBean(SolverFactory.class);
                    var problem =
                            ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritanceinterface.baseannotated.childannotated.TestdataMultipleSolution
                                    .generateSolution(1, 2, false);
                    var solution =
                            (ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritanceinterface.baseannotated.childannotated.TestdataMultipleSolution) solverFactory
                                    .buildSolver().solve(problem);
                    assertNotNull(solution);
                    assertThat(solution.getScore()).isEqualTo(SimpleScore.of(4)); // value of 2 for each constraint
                }))
                .doesNotThrowAnyException();
    }

    /**
     * This test validates the behavior of the solver
     * when only the base class is annotated with {@code @PlanningEntity} and multiple inheritance is used.
     */
    @Test
    void testMultipleOnlyBaseClassAnnotated() {
        assertThatCode(() -> multipleOnlyBaseClassAnnotatedContextRunner
                .run(context -> {
                    var solverFactory = context.getBean(SolverFactory.class);
                    var problem =
                            ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritance.baseannotated.childnotannotated.TestdataMultipleSolution
                                    .generateSolution(1, 2);
                    var solution =
                            (ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritance.baseannotated.childnotannotated.TestdataMultipleSolution) solverFactory
                                    .buildSolver().solve(problem);
                    assertNotNull(solution);
                    assertThat(solution.getScore()).isEqualTo(SimpleScore.of(2)); // value of 2 for each constraint
                }))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * This test validates the behavior of the solver
     * when only the base class is annotated with {@code @PlanningEntity}, multiple inheritance is used,
     * and the base entity is an interface.
     */
    @Test
    void testMultipleOnlyBaseClassAnnotatedBaseIsInterface() {
        assertThatCode(() -> multipleOnlyBaseClassAnnotatedBaseInterfaceContextRunner
                .run(context -> {
                    var solverFactory = context.getBean(SolverFactory.class);
                    var problem =
                            ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritanceinterface.baseannotated.childnotannotated.TestdataMultipleSolution
                                    .generateSolution(1, 2);
                    var solution =
                            (ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritanceinterface.baseannotated.childnotannotated.TestdataMultipleSolution) solverFactory
                                    .buildSolver().solve(problem);
                    assertNotNull(solution);
                    assertThat(solution.getScore()).isEqualTo(SimpleScore.of(2)); // value of 2 for each constraint
                }))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * This test validates the behavior of the solver
     * when multiple inheritance is used, the child is annotated with {@code @PlanningEntity}
     * and it inherits from class and interface.
     */
    @Test
    void testMultipleBothClassesAnnotatedMixedPattern() {
        assertThatCode(() -> multipleBothClassesAnnotatedMixedPatternContextRunner
                .run(context -> {
                    var solverFactory = context.getBean(SolverFactory.class);
                    var problem =
                            ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritance.baseannotated.mixed.childannotated.TestdataMultipleSolution
                                    .generateSolution(1, 2, false);
                    var solution =
                            (ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritance.baseannotated.mixed.childannotated.TestdataMultipleSolution) solverFactory
                                    .buildSolver().solve(problem);
                    assertNotNull(solution);
                    assertThat(solution.getScore()).isEqualTo(SimpleScore.of(6)); // value of 2 for each constraint
                }))
                .doesNotThrowAnyException();
    }
}
