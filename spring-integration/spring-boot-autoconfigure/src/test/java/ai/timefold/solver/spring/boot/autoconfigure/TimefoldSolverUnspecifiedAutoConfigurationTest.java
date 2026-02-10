package ai.timefold.solver.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThatCode;

import ai.timefold.solver.spring.boot.autoconfigure.dummy.NoEntitySpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.NoSolutionSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.invalid.solution.InvalidSolutionSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.NoConstraintsSpringTestConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.TestExecutionListeners;

@TestExecutionListeners
@Execution(ExecutionMode.CONCURRENT)
class TimefoldSolverUnspecifiedAutoConfigurationTest {

    private final ApplicationContextRunner noUserConfigurationContextRunner;

    public TimefoldSolverUnspecifiedAutoConfigurationTest() {
        noUserConfigurationContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class));
    }

    @Test
    void noSolutionClass() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(NoSolutionSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains("No classes were found with a @PlanningSolution annotation.");
    }

    @Test
    void noEntityClass() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(NoEntitySpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains("No classes were found with a @PlanningEntity annotation.");
    }

    @Test
    void noConstraintClass() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(NoConstraintsSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "No classes found that implement EasyScoreCalculator, ConstraintProvider, or IncrementalScoreCalculator.");
    }

    @Test
    void invalidSolution() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(InvalidSolutionSpringTestConfiguration.class)
                .withPropertyValues(
                        "timefold.solver.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/invalidSolverConfig.xml")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains("cannot be a record as it needs to be mutable.");
    }

}
