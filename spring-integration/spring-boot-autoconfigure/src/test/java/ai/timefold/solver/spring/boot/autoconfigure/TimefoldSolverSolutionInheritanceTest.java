package ai.timefold.solver.spring.boot.autoconfigure;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childnot.TestdataOnlyBaseAnnotatedExtendedSolution;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtooabstract.TestdataBothAnnotatedAbstractExtendedSolution;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.solution.BothAnnotatedAbstractSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.solution.BothAnnotatedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.solution.OnlyBaseAnnotatedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.inheritance.solution.OnlyChildAnnotatedSpringTestConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.TestExecutionListeners;

@TestExecutionListeners
class TimefoldSolverSolutionInheritanceTest {

    private final ApplicationContextRunner bothClassesAnnotatedContextRunner;
    private final ApplicationContextRunner bothClassesAnnotatedAbstractContextRunner;
    private final ApplicationContextRunner onlyChildClassAnnotatedContextRunner;
    private final ApplicationContextRunner onlyBaseClassAnnotatedContextRunner;

    public TimefoldSolverSolutionInheritanceTest() {
        bothClassesAnnotatedContextRunner = new ApplicationContextRunner()
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
     * This test validates the behavior of the solver
     * when only child class is annotated with {@code @PlanningSolution}.
     */
    @Test
    void testOnlyChildClassAnnotated() {
        assertThatCode(() -> onlyChildClassAnnotatedContextRunner
                .run(context -> context.getBean(SolverFactory.class)))
                .rootCause()
                .hasMessageContaining(
                        "Maybe add a getScore() method with a @PlanningScore annotation.");
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
}
