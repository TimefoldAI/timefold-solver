package ai.timefold.solver.spring.boot.autoconfigure;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childnot.TestdataOnlyBaseAnnotatedExtendedSolution;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedExtendedSolution;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtooabstract.TestdataBothAnnotatedAbstractExtendedSolution;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.solution.BothAnnotatedAbstractSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.solution.BothAnnotatedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.solution.MultipleInheritanceSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.solution.OnlyBaseAnnotatedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.solution.OnlyChildAnnotatedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.solution.ReplaceAnnotatedMemberSpringTestConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.TestExecutionListeners;

@TestExecutionListeners
@Execution(ExecutionMode.CONCURRENT)
class TimefoldSolverSolutionInheritanceTest {

    private final ApplicationContextRunner bothClassesAnnotatedContextRunner;
    private final ApplicationContextRunner bothClassesAnnotatedConfiguredXmlContextRunner;
    private final ApplicationContextRunner bothClassesAnnotatedAbstractContextRunner;
    private final ApplicationContextRunner onlyChildClassAnnotatedContextRunner;
    private final ApplicationContextRunner onlyBaseClassAnnotatedContextRunner;
    private final ApplicationContextRunner replaceMemberContextRunner;
    private final ApplicationContextRunner multipleInheritanceContextRunner;

    public TimefoldSolverSolutionInheritanceTest() {
        bothClassesAnnotatedContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(BothAnnotatedSpringTestConfiguration.class);
        bothClassesAnnotatedConfiguredXmlContextRunner = new ApplicationContextRunner()
                .withPropertyValues(
                        "timefold.solver.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/inheritance/bothClassAnnotatedConfig.xml")
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(BothAnnotatedSpringTestConfiguration.class);
        bothClassesAnnotatedAbstractContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(BothAnnotatedAbstractSpringTestConfiguration.class);
        onlyChildClassAnnotatedContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(OnlyChildAnnotatedSpringTestConfiguration.class);
        onlyBaseClassAnnotatedContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(OnlyBaseAnnotatedSpringTestConfiguration.class);
        replaceMemberContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(ReplaceAnnotatedMemberSpringTestConfiguration.class);
        multipleInheritanceContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MultipleInheritanceSpringTestConfiguration.class);
    }

    /**
     * This test validates the behavior of the solver
     * when both child and parent solution classes are annotated with {@code @PlanningSolution}.
     */
    @Test
    void testBothClassesAnnotated() {
        assertThatCode(() -> bothClassesAnnotatedContextRunner
                .run(context -> context.getBean(SolverFactory.class)))
                .rootCause()
                .hasMessageContaining("Multiple classes", "found in the classpath with a @PlanningSolution annotation.");
    }

    /**
     * This test validates the behavior of the solver
     * when both child and parent solution classes are annotated with {@code @PlanningSolution},
     * and the base class is abstract.
     */
    @Test
    void testBothClassesAnnotatedBaseAbstract() {
        assertThatCode(() -> bothClassesAnnotatedAbstractContextRunner
                .run(context -> {
                    var solverFactory = context.getBean(SolverFactory.class);
                    var problem = TestdataBothAnnotatedAbstractExtendedSolution.generateSolution(3, 2);
                    var solution = (TestdataBothAnnotatedAbstractExtendedSolution) solverFactory.buildSolver().solve(problem);
                    assertNotNull(solution);
                    assertThat(solution.getScore()).isEqualTo(SimpleScore.of(8));
                }))
                .doesNotThrowAnyException();
    }

    /**
     * Same as {@link #testBothClassesAnnotated()}, but it uses XML configuration.
     */
    @Test
    void testBothClassesAnnotatedConfiguredXml() {
        assertThatCode(() -> bothClassesAnnotatedConfiguredXmlContextRunner
                .run(context -> {
                    var solverFactory = context.getBean(SolverFactory.class);
                    var problem = TestdataBothAnnotatedExtendedSolution.generateSolution(3, 2);
                    var solution = (TestdataBothAnnotatedExtendedSolution) solverFactory.buildSolver().solve(problem);
                    assertNotNull(solution);
                    assertThat(solution.getScore()).isEqualTo(SimpleScore.of(16));
                }))
                .doesNotThrowAnyException();
    }

    /**
     * This test validates the behavior of the solver
     * when only child class is annotated with {@code @PlanningSolution}.
     */
    @Test
    void testOnlyChildClassAnnotated() {
        assertThatCode(() -> onlyChildClassAnnotatedContextRunner
                .run(context -> context.getBean(SolverFactory.class)))
                .rootCause()
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
        assertThatCode(() -> onlyBaseClassAnnotatedContextRunner
                .run(context -> {
                    var solverFactory = context.getBean(SolverFactory.class);
                    var problem = TestdataOnlyBaseAnnotatedExtendedSolution.generateSolution(3, 2);
                    var solution = (TestdataOnlyBaseAnnotatedExtendedSolution) solverFactory.buildSolver().solve(problem);
                    assertNotNull(solution);
                    assertThat(solution.getScore()).isEqualTo(SimpleScore.of(2));
                }))
                .doesNotThrowAnyException();
    }

    /**
     * This test validates the behavior of the solver
     * when the child {@code @PlanningSolution} replaces an existing annotated member.
     */
    @Test
    void testReplaceAnnotatedMember() {
        assertThatCode(() -> replaceMemberContextRunner
                .run(context -> context.getBean(SolverFactory.class)))
                .rootCause()
                .hasMessageContaining("Multiple classes")
                .hasMessageContaining("found in the classpath with a @PlanningSolution annotation.");
    }

    /**
     * This test validates the behavior of the solver
     * when multiple inheritance is applied.
     */
    @Test
    void testMultipleInheritance() {
        assertThatCode(() -> multipleInheritanceContextRunner
                .run(context -> context.getBean(SolverFactory.class)))
                .rootCause()
                .hasMessageContaining("Multiple classes")
                .hasMessageContaining("found in the classpath with a @PlanningSolution annotation.");
    }
}
