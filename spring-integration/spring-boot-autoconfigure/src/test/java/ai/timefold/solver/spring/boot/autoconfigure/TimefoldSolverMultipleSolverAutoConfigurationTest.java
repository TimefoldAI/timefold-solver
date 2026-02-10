package ai.timefold.solver.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.score.ScoreManager;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverConfigOverride;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.solver.DefaultSolverJob;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.spring.boot.autoconfigure.basic.EmptySpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.basic.NoConstraintsSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.basic.NormalSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.basic.constraints.TestdataSpringConstraintProvider;
import ai.timefold.solver.spring.boot.autoconfigure.basic.domain.TestdataSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.basic.domain.TestdataSpringSolution;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;
import ai.timefold.solver.spring.boot.autoconfigure.declarative.constraints.TestdataSpringSupplierVariableConstraintProvider;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.MultipleConstraintProviderSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.MultipleEasyScoreConstraintSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.MultipleIncrementalScoreConstraintSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.MultipleSolutionsSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.NoEntitySpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.NoSolutionSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.basic.constraints.easy.DummySpringEasyScore;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.basic.constraints.incremental.DummySpringIncrementalScore;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.list.constraints.easy.DummySpringListEasyScore;
import ai.timefold.solver.spring.boot.autoconfigure.dummy.list.constraints.incremental.DummySpringListIncrementalScore;
import ai.timefold.solver.spring.boot.autoconfigure.gizmo.domain.TestdataGizmoSpringSolution;
import ai.timefold.solver.spring.boot.autoconfigure.invalid.entity.InvalidEntitySpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.invalid.solution.InvalidSolutionSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.invalid.type.InvalidEntityTypeSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.multimodule.MultiModuleSpringTestConfiguration;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestExecutionListeners;

@TestExecutionListeners
@Execution(ExecutionMode.CONCURRENT)
@ResourceLock("yamlAndXml")
class TimefoldSolverMultipleSolverAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner;
    private final ApplicationContextRunner emptyContextRunner;
    private final ApplicationContextRunner noUserConfigurationContextRunner;
    private final ApplicationContextRunner multimoduleRunner;
    private final FilteredClassLoader allDefaultsFilteredClassLoader;

    public TimefoldSolverMultipleSolverAutoConfigurationTest() {
        contextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(NormalSpringTestConfiguration.class);
        emptyContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(EmptySpringTestConfiguration.class);
        multimoduleRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MultiModuleSpringTestConfiguration.class);
        allDefaultsFilteredClassLoader =
                new FilteredClassLoader(FilteredClassLoader.PackageFilter.of("ai.timefold.solver.test"),
                        FilteredClassLoader.ClassPathResourceFilter
                                .of(new ClassPathResource(TimefoldProperties.DEFAULT_SOLVER_CONFIG_URL)));
        noUserConfigurationContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class));
    }

    @Test
    void noSolutionOrEntityClasses() {
        emptyContextRunner
                .withPropertyValues("timefold.solver.solver1.termination.spent-limit=4h")
                .withPropertyValues("timefold.solver.solver2.termination.spent-limit=4h")
                .run(context -> assertThat(context.getStartupFailure()).isNull());
    }

    @Test
    void solverConfigXml_none() {
        contextRunner
                .withPropertyValues("timefold.solver.solver1.termination.spent-limit=10s")
                .withPropertyValues("timefold.solver.solver2.termination.spent-limit=20s")
                .withClassLoader(allDefaultsFilteredClassLoader)
                .run(context -> {
                    var solver1 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver1");
                    var solver2 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver2");
                    assertThat(solver1).isNotNull();
                    assertThat(solver2).isNotNull();
                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());
                    SolverScope<TestdataSpringSolution> customScope = new SolverScope<>() {
                        @Override
                        public long calculateTimeMillisSpentUpToNow() {
                            // Return five seconds to make the time gradient predictable
                            return 5000L;
                        }
                    };
                    // We ensure the best-score limit won't take priority
                    customScope.setStartingInitializedScore(HardSoftScore.of(-1, -1));
                    customScope.setInitializedBestScore(HardSoftScore.of(-1, -1));
                    var gradientTimeDefaultSolver1 =
                            ((DefaultSolverJob<TestdataSpringSolution, Long>) solver1.solve(1L, problem)).getSolverTermination()
                                    .calculateSolverTimeGradient(customScope);
                    assertThat(gradientTimeDefaultSolver1).isEqualTo(0.5);
                    var gradientTimeSolver2 =
                            ((DefaultSolverJob<TestdataSpringSolution, Long>) solver2.solve(1L, problem)).getSolverTermination()
                                    .calculateSolverTimeGradient(customScope);
                    assertThat(gradientTimeSolver2).isEqualTo(0.25);
                });
    }

    @Test
    void solverConfigXml_property() {
        contextRunner
                .withPropertyValues(
                        "timefold.solver.solver1.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/customSolver1Config.xml")
                .withPropertyValues(
                        "timefold.solver.solver2.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/customSolver2Config.xml")
                .run(context -> {
                    var solver1 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver1");
                    var solver2 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver2");
                    assertThat(solver1).isNotNull();
                    assertThat(solver2).isNotNull();
                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());
                    SolverScope<TestdataSpringSolution> customScope = new SolverScope<>() {
                        @Override
                        public long calculateTimeMillisSpentUpToNow() {
                            // Return five seconds to make the time gradient predictable
                            return 5000L;
                        }
                    };
                    // We ensure the best-score limit won't take priority
                    customScope.setStartingInitializedScore(HardSoftScore.of(-1, -1));
                    customScope.setInitializedBestScore(HardSoftScore.of(-1, -1));
                    var gradientTimeDefaultSolver1 =
                            ((DefaultSolverJob<TestdataSpringSolution, Long>) solver1.solve(1L, problem)).getSolverTermination()
                                    .calculateSolverTimeGradient(customScope);
                    assertThat(gradientTimeDefaultSolver1).isEqualTo(0.5);
                    var gradientTimeSolver2 =
                            ((DefaultSolverJob<TestdataSpringSolution, Long>) solver2.solve(1L, problem)).getSolverTermination()
                                    .calculateSolverTimeGradient(customScope);
                    assertThat(gradientTimeSolver2).isEqualTo(0.25);
                });
    }

    @Test
    void solverConfigXml_property_noGlobalTermination() {
        contextRunner
                .withPropertyValues(
                        "timefold.solver.solver1.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/solverConfigWithoutGlobalTermination.xml")
                .withPropertyValues(
                        "timefold.solver.solver2.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/solverConfigWithoutGlobalTermination.xml")
                .run(context -> {
                    var solver1 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver1");
                    var solver2 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver2");
                    assertThat(solver1).isNotNull();
                    assertThat(solver2).isNotNull();
                });
    }

    @Test
    void solverProperties() {
        contextRunner
                .withPropertyValues("timefold.solver.solver1.environment-mode=FULL_ASSERT")
                .withPropertyValues("timefold.solver.solver2.environment-mode=TRACKED_FULL_ASSERT")
                .run(context -> {
                    var solver1 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver1");
                    var solver2 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver2");
                    assertThat(solver1).isNotNull();
                    assertThat(solver2).isNotNull();
                });
        contextRunner
                .withPropertyValues("timefold.solver.solver1.daemon=true")
                .withPropertyValues("timefold.solver.solver2.daemon=false")
                .run(context -> {
                    var solver1 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver1");
                    var solver2 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver2");
                    assertThat(solver1).isNotNull();
                    assertThat(solver2).isNotNull();
                });
    }

    @Test
    void invalidNearbyClass() {
        // Class not found
        assertThatCode(() -> contextRunner
                .withPropertyValues("timefold.solver.solver1.daemon=true")
                .withPropertyValues(
                        "timefold.solver.solver1.nearby-distance-meter-class=ai.timefold.solver.spring.boot.autoconfigure.dummy.BadDummyDistanceMeter")
                .withPropertyValues("timefold.solver.solver2.daemon=false")
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getNearbyDistanceMeterClass()).isNotNull();
                }))
                .rootCause().message().contains("Cannot find the Nearby Selection Meter class",
                        "ai.timefold.solver.spring.boot.autoconfigure.dummy.BadDummyDistanceMeter");
        // Invalid class
        assertThatCode(() -> contextRunner
                .withPropertyValues("timefold.solver.solver1.daemon=true")
                .withPropertyValues("timefold.solver.solver2.daemon=false")
                .withPropertyValues(
                        "timefold.solver.solver2.nearby-distance-meter-class=ai.timefold.solver.spring.boot.autoconfigure.basic.domain.TestdataSpringSolution")
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getNearbyDistanceMeterClass()).isNotNull();
                }))
                .rootCause().message().contains("The Nearby Selection Meter class",
                        "ai.timefold.solver.spring.boot.autoconfigure.basic.domain.TestdataSpringSolution");
    }

    @Test
    void solverPropertiesWithoutLoadingBenchmark() {
        contextRunner
                .withPropertyValues("timefold.solver.solver1.environment-mode=FULL_ASSERT")
                .withPropertyValues("timefold.solver.solver2.environment-mode=TRACKED_FULL_ASSERT")
                .withUserConfiguration(TimefoldBenchmarkAutoConfiguration.class) // We load the configuration, but get no bean
                .run(context -> {
                    var solver1 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver1");
                    var solver2 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver2");
                    assertThat(solver1).isNotNull();
                    assertThat(solver2).isNotNull();
                });
    }

    @Test
    void solve() {
        contextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> {
                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());

                    for (var solverName : List.of("solver1", "solver2")) {
                        var solver =
                                (SolverManager<TestdataSpringSolution, Long>) context.getBean(solverName);
                        var solverJob = solver.solve(1L, problem);
                        var solution = solverJob.getFinalBestSolution();
                        assertThat(solution).isNotNull();
                        assertThat(solution.getScore().score()).isNotNegative();
                    }
                });
    }

    @Test
    void solverWithYaml() {
        contextRunner
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withSystemProperties(
                        "spring.config.location=classpath:ai/timefold/solver/spring/boot/autoconfigure/multiple-solvers/application.yaml")
                .run(context -> {
                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());

                    for (var solverName : List.of("solver1", "solver2")) {
                        var solver =
                                (SolverManager<TestdataSpringSolution, Long>) context.getBean(solverName);
                        var solverJob = solver.solve(1L, problem);
                        var solution = solverJob.getFinalBestSolution();
                        assertThat(solution).isNotNull();
                        assertThat(solution.getScore().score()).isNotNegative();
                    }
                });

        assertThatCode(() -> contextRunner
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withSystemProperties(
                        "spring.config.location=classpath:ai/timefold/solver/spring/boot/autoconfigure/multiple-solvers/invalid-application.yaml")
                .run(context -> context.getBean(SolverConfig.class)))
                .rootCause().message().contains("The properties", "solverConfigXml", "environmentMode", "moveThreadCount",
                        "domainAccessType", "are not valid", "Maybe try changing the property name to kebab-case");
        assertThatCode(() -> contextRunner
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withSystemProperties(
                        "spring.config.location=classpath:ai/timefold/solver/spring/boot/autoconfigure/multiple-solvers/invalid-termination-application.yaml")
                .run(context -> context.getBean(SolverConfig.class)))
                .rootCause().message().contains("The termination properties", "spentLimit", "unimprovedSpentLimit",
                        "bestScoreLimit", "are not valid", "Maybe try changing the property name to kebab-case");
    }

    @Test
    void solveWithTimeOverride() {
        contextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0",
                        "timefold.solver.solver1.termination.spent-limit=30s")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0",
                        "timefold.solver.solver2.termination.spent-limit=30s")
                .run(context -> {
                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());
                    for (var solverName : List.of("solver1", "solver2")) {
                        var solverManager =
                                (SolverManager<TestdataSpringSolution, Long>) context.getBean(solverName);
                        var solverJob =
                                (DefaultSolverJob<TestdataSpringSolution, Long>) solverManager.solveBuilder()
                                        .withProblemId(1L)
                                        .withProblem(problem)
                                        .withConfigOverride(
                                                new SolverConfigOverride<TestdataSpringSolution>()
                                                        .withTerminationConfig(new TerminationConfig()
                                                                .withSpentLimit(Duration.ofSeconds(2L))))
                                        .run();
                        SolverScope<TestdataSpringSolution> customScope = new SolverScope<>() {
                            @Override
                            public long calculateTimeMillisSpentUpToNow() {
                                // Return one second to make the time gradient predictable
                                return 1000L;
                            }
                        };
                        // We ensure the best-score limit won't take priority
                        customScope.setStartingInitializedScore(HardSoftScore.of(-1, -1));
                        customScope.setInitializedBestScore(HardSoftScore.of(-1, -1));
                        var gradientTime = solverJob.getSolverTermination().calculateSolverTimeGradient(customScope);
                        var solution = solverJob.getFinalBestSolution();
                        assertThat(solution).isNotNull();
                        assertThat(solution.getScore().score()).isNotNegative();
                        // Spent-time is 30s by default, but it is overridden with 2. The gradient time must be 50%
                        assertThat(gradientTime).isEqualTo(0.5);
                    }
                });
    }

    @Test
    void multimoduleSolve() {
        multimoduleRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> {
                    for (var solverName : List.of("solver1", "solver2")) {
                        var solverManager =
                                (SolverManager<TestdataSpringSolution, Long>) context.getBean(solverName);
                        var problem = new TestdataSpringSolution();
                        problem.setValueList(IntStream.range(1, 3)
                                .mapToObj(i -> "v" + i)
                                .toList());
                        problem.setEntityList(IntStream.range(1, 3)
                                .mapToObj(i -> new TestdataSpringEntity())
                                .toList());
                        var solverJob = solverManager.solve(1L, problem);
                        var solution = solverJob.getFinalBestSolution();
                        assertThat(solution).isNotNull();
                        assertThat(solution.getScore().score()).isNotNegative();
                    }
                });
    }

    @Test
    void resoucesInjectionFailure() {
        assertThatCode(() -> contextRunner
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> context.getBean(SolverConfig.class)))
                .hasMessageContaining(
                        "No qualifying bean of type 'ai.timefold.solver.core.config.solver.SolverConfig' available");
        assertThatCode(() -> contextRunner
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> context.getBean(SolverFactory.class)))
                .hasRootCauseMessage(
                        "No qualifying bean of type 'ai.timefold.solver.core.api.solver.SolverFactory' available");
        assertThatCode(() -> contextRunner
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> context.getBean(SolverManager.class)))
                .hasMessageContaining(
                        "No qualifying bean of type 'ai.timefold.solver.core.api.solver.SolverManager' available");
        assertThatCode(() -> contextRunner
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> context.getBean(ScoreManager.class)))
                .hasMessageContaining(
                        "No qualifying bean of type 'ai.timefold.solver.core.api.score.ScoreManager' available");
        assertThatCode(() -> contextRunner
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> context.getBean(SolutionManager.class)))
                .hasMessageContaining(
                        "No qualifying bean of type 'ai.timefold.solver.core.api.solver.SolutionManager' available");
        assertThatCode(() -> contextRunner
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> context.getBean(ConstraintVerifier.class)))
                .hasMessageContaining(
                        "No qualifying bean of type 'ai.timefold.solver.core.api.score.stream.ConstraintProvider' available");
    }

    @Test
    void noSolutionClass() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(NoSolutionSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains("No classes were found with a @PlanningSolution annotation.");
    }

    @Test
    void multipleSolutionClasses() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleSolutionsSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Some solver configs", "solver2", "solver1",
                        "don't specify a PlanningSolution class, yet there are multiple available",
                        TestdataGizmoSpringSolution.class.getSimpleName(),
                        TestdataSpringSolution.class.getSimpleName(),
                        "on the classpath.");
    }

    @Test
    void unusedSolutionClass() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleSolutionsSpringTestConfiguration.class)
                .withPropertyValues(
                        "timefold.solver.solver1.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/normalSolverConfig.xml")
                .withPropertyValues(
                        "timefold.solver.solver2.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/normalSolverConfig.xml")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Unused classes ([" + TestdataGizmoSpringSolution.class.getCanonicalName()
                                + "]) found with a @PlanningSolution annotation.");
    }

    @Test
    void noEntityClass() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(NoEntitySpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains("No classes were found with a @PlanningEntity annotation.");
    }

    @Test
    void noConstraintClass() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(NoConstraintsSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "No classes found that implement EasyScoreCalculator, ConstraintProvider, or IncrementalScoreCalculator.");
    }

    @Test
    void multipleEasyScoreConstraints() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleEasyScoreConstraintSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Some solver configs", "solver2", "solver1",
                        "don't specify a EasyScoreCalculator score calculator class, yet there are multiple available",
                        DummySpringEasyScore.class.getSimpleName(),
                        DummySpringListEasyScore.class.getSimpleName(),
                        "on the classpath.");
    }

    @Test
    void multipleConstraintProviderConstraints() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleConstraintProviderSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Some solver configs", "solver2", "solver1",
                        "don't specify a ConstraintProvider score calculator class, yet there are multiple available",
                        TestdataSpringConstraintProvider.class.getSimpleName(),
                        TestdataSpringSupplierVariableConstraintProvider.class.getSimpleName(),
                        "on the classpath.");
    }

    @Test
    void multipleIncrementalScoreConstraints() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleIncrementalScoreConstraintSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Some solver configs", "solver2", "solver1",
                        "don't specify a IncrementalScoreCalculator score calculator class, yet there are multiple available",
                        DummySpringIncrementalScore.class.getSimpleName(),
                        DummySpringListIncrementalScore.class.getSimpleName(),
                        "on the classpath.");
    }

    @Test
    void multipleEasyScoreConstraintsXml_property() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleEasyScoreConstraintSpringTestConfiguration.class)
                .withPropertyValues(
                        "timefold.solver.solver1.solver-config-xml=solverConfig.xml")
                .withPropertyValues(
                        "timefold.solver.solver2.solver-config-xml=solverConfig.xml")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Some solver configs", "solver2", "solver1",
                        "don't specify a EasyScoreCalculator score calculator class, yet there are multiple available",
                        DummySpringEasyScore.class.getSimpleName(),
                        DummySpringListEasyScore.class.getSimpleName(),
                        "on the classpath.");
    }

    @Test
    void multipleConstraintProviderConstraintsXml_property() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleConstraintProviderSpringTestConfiguration.class)
                .withPropertyValues(
                        "timefold.solver.solver1.solver-config-xml=solverConfig.xml")
                .withPropertyValues(
                        "timefold.solver.solver2.solver-config-xml=solverConfig.xml")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Some solver configs", "solver2", "solver1",
                        "don't specify a ConstraintProvider score calculator class, yet there are multiple available",
                        TestdataSpringConstraintProvider.class.getSimpleName(),
                        TestdataSpringSupplierVariableConstraintProvider.class.getSimpleName(),
                        "on the classpath.");
    }

    @Test
    void multipleIncrementalScoreConstraintsXml_property() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleIncrementalScoreConstraintSpringTestConfiguration.class)
                .withPropertyValues(
                        "timefold.solver.solver1.solver-config-xml=solverConfig.xml")
                .withPropertyValues(
                        "timefold.solver.solver2.solver-config-xml=solverConfig.xml")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Some solver configs", "solver2", "solver1",
                        "don't specify a IncrementalScoreCalculator score calculator class, yet there are multiple available",
                        DummySpringIncrementalScore.class.getSimpleName(),
                        DummySpringListIncrementalScore.class.getSimpleName(),
                        "on the classpath.");
    }

    @Test
    void unusedEasyScoreConstraints() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleEasyScoreConstraintSpringTestConfiguration.class)
                .withPropertyValues(
                        "timefold.solver.solver1.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/easyScoreSolverConfig.xml")
                .withPropertyValues(
                        "timefold.solver.solver2.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/easyScoreSolverConfig.xml")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Unused classes ([" + DummySpringListEasyScore.class.getCanonicalName()
                                + "]) that implements EasyScoreCalculator were found.");
    }

    @Test
    void unusedConstraintProviderConstraints() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleConstraintProviderSpringTestConfiguration.class)
                .withPropertyValues(
                        "timefold.solver.solver1.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/normalSolverConfig.xml")
                .withPropertyValues(
                        "timefold.solver.solver2.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/normalSolverConfig.xml")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Unused classes ([" + TestdataSpringSupplierVariableConstraintProvider.class.getCanonicalName()
                                + "]) that implements ConstraintProvider were found.");
    }

    @Test
    void unusedIncrementalScoreConstraints() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(MultipleIncrementalScoreConstraintSpringTestConfiguration.class)
                .withPropertyValues(
                        "timefold.solver.solver1.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/incrementalScoreSolverConfig.xml")
                .withPropertyValues(
                        "timefold.solver.solver2.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/incrementalScoreSolverConfig.xml")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "Unused classes ([" + DummySpringListIncrementalScore.class.getCanonicalName()
                                + "]) that implements IncrementalScoreCalculator were found.");
    }

    @Test
    void invalidEntity() {
        assertThatCode(() -> contextRunner
                .withUserConfiguration(InvalidEntityTypeSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains("is not annotated with @PlanningEntity but defines genuine or shadow variables.");

        assertThatCode(() -> contextRunner
                .withUserConfiguration(InvalidEntitySpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains("as it needs to be mutable.");
    }

    @Test
    void invalidSolution() {
        assertThatCode(() -> noUserConfigurationContextRunner
                .withUserConfiguration(InvalidSolutionSpringTestConfiguration.class)
                .withPropertyValues(
                        "timefold.solver.solver1.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/invalidSolverConfig.xml")
                .withPropertyValues(
                        "timefold.solver.solver2.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/invalidSolverConfig.xml")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains("cannot be a record as it needs to be mutable.");
    }
}
