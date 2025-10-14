package ai.timefold.solver.spring.boot.autoconfigure;

import ai.timefold.solver.spring.boot.autoconfigure.chained.constraints.TestdataChainedSpringConstraintProvider;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.MultipleConstraintProviderSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.MultipleEasyScoreConstraintSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.MultipleIncrementalScoreConstraintSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.MultipleSolutionsSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.NoEntitySpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.NoSolutionSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.chained.constraints.easy.DummyChainedSpringEasyScore;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.chained.constraints.incremental.DummyChainedSpringIncrementalScore;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.normal.constraints.easy.DummySpringEasyScore;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.normal.constraints.incremental.DummySpringIncrementalScore;
import ai.timefold.solver.spring.boot.autoconfigure.invalid.solution.InvalidSolutionSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.NoConstraintsSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.constraints.TestdataSpringConstraintProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.TestExecutionListeners;

import static org.assertj.core.api.Assertions.assertThatCode;

@TestExecutionListeners
@Execution(ExecutionMode.CONCURRENT)
class TimefoldSolverNoUserAutoConfigurationTest {

    private final ApplicationContextRunner noUserConfigurationContextRunner;

    public TimefoldSolverNoUserAutoConfigurationTest() {
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
    void multipleSolutionClasses() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleSolutionsSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Multiple classes", "TestdataChainedSpringSolution", "TestdataSpringSolution",
                        "found in the classpath with a @PlanningSolution annotation.");
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
    void multipleEasyScoreConstraints() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleEasyScoreConstraintSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Multiple score calculator classes", DummyChainedSpringEasyScore.class.getSimpleName(),
                        DummySpringEasyScore.class.getSimpleName(),
                        "that implements EasyScoreCalculator were found in the classpath.");
    }

    @Test
    void multipleConstraintProviderConstraints() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleConstraintProviderSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Multiple score calculator classes", TestdataChainedSpringConstraintProvider.class.getSimpleName(),
                        TestdataSpringConstraintProvider.class.getSimpleName(),
                        "that implements ConstraintProvider were found in the classpath.");
    }

    @Test
    void multipleIncrementalScoreConstraints() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleIncrementalScoreConstraintSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Multiple score calculator classes", DummyChainedSpringIncrementalScore.class.getSimpleName(),
                        DummySpringIncrementalScore.class.getSimpleName(),
                        "that implements IncrementalScoreCalculator were found in the classpath.");
    }

    @Test
    void multipleEasyScoreConstraintsXml_property() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleEasyScoreConstraintSpringTestConfiguration.class)
                .withPropertyValues(
                        "timefold.solver.solver.solver-config-xml=solverConfig.xml")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Multiple score calculator classes",
                        DummyChainedSpringEasyScore.class.getSimpleName(),
                        DummySpringEasyScore.class.getSimpleName(),
                        "that implements EasyScoreCalculator were found in the classpath");
    }

    @Test
    void multipleConstraintProviderConstraintsXml_property() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleConstraintProviderSpringTestConfiguration.class)
                .withPropertyValues(
                        "timefold.solver.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/normalSolverConfig.xml")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Multiple score calculator classes", TestdataChainedSpringConstraintProvider.class.getSimpleName(),
                        TestdataSpringConstraintProvider.class.getSimpleName(),
                        "that implements ConstraintProvider were found in the classpath.");
    }

    @Test
    void multipleIncrementalScoreConstraintsXml_property() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleIncrementalScoreConstraintSpringTestConfiguration.class)
                .withPropertyValues(
                        "timefold.solver.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/normalSolverConfig.xml")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Multiple score calculator classes", DummyChainedSpringIncrementalScore.class.getSimpleName(),
                        DummySpringIncrementalScore.class.getSimpleName(),
                        "that implements IncrementalScoreCalculator were found in the classpath.");
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
