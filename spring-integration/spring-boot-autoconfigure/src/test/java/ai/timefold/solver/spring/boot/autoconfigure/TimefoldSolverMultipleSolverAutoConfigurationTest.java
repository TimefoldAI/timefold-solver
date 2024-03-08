package ai.timefold.solver.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.core.api.score.ScoreManager;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverConfigOverride;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.solver.DefaultSolverJob;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.spring.boot.autoconfigure.chained.ChainedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.chained.constraints.TestdataChainedSpringConstraintProvider;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringSolution;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;
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
import ai.timefold.solver.spring.boot.autoconfigure.gizmo.GizmoSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.invalid.entity.InvalidEntitySpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.invalid.solution.InvalidSolutionSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.invalid.type.InvalidEntityTypeSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.multimodule.MultiModuleSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.EmptySpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.NoConstraintsSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.NormalSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.constraints.TestdataSpringConstraintProvider;
import ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringSolution;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestExecutionListeners;

@TestExecutionListeners
class TimefoldSolverMultipleSolverAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner;
    private final ApplicationContextRunner emptyContextRunner;
    private final ApplicationContextRunner noUserConfigurationContextRunner;
    private final ApplicationContextRunner benchmarkContextRunner;
    private final ApplicationContextRunner chainedContextRunner;
    private final ApplicationContextRunner gizmoContextRunner;
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
        benchmarkContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class,
                                TimefoldBenchmarkAutoConfiguration.class))
                .withUserConfiguration(NormalSpringTestConfiguration.class);
        gizmoContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(GizmoSpringTestConfiguration.class);
        chainedContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(ChainedSpringTestConfiguration.class);
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
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNull();
                });
    }

    @Test
    void solverConfigXml_none() {
        contextRunner
                .withPropertyValues("timefold.solver.solver1.termination.spent-limit=10s")
                .withPropertyValues("timefold.solver.solver2.termination.spent-limit=20s")
                .withClassLoader(allDefaultsFilteredClassLoader)
                .run(context -> {
                    SolverManager<TestdataSpringSolution, Long> solver1 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver1");
                    SolverManager<TestdataSpringSolution, Long> solver2 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver2");
                    assertThat(solver1).isNotNull();
                    assertThat(solver2).isNotNull();
                    TestdataSpringSolution problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .collect(Collectors.toList()));
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .collect(Collectors.toList()));
                    SolverScope<TestdataSpringSolution> customScope = new SolverScope<>() {
                        @Override
                        public long calculateTimeMillisSpentUpToNow() {
                            // Return five seconds to make the time gradient predictable
                            return 5000L;
                        }
                    };
                    // We ensure the best-score limit won't take priority
                    customScope.setStartingInitializedScore(HardSoftScore.of(-1, -1));
                    customScope.setBestScore(HardSoftScore.of(-1, -1));
                    double gradientTimeDefaultSolver1 =
                            ((DefaultSolverJob<TestdataSpringSolution, Long>) solver1.solve(1L, problem)).getSolverTermination()
                                    .calculateSolverTimeGradient(customScope);
                    assertThat(gradientTimeDefaultSolver1).isEqualTo(0.5);
                    double gradientTimeSolver2 =
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
                    SolverManager<TestdataSpringSolution, Long> solver1 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver1");
                    SolverManager<TestdataSpringSolution, Long> solver2 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver2");
                    assertThat(solver1).isNotNull();
                    assertThat(solver2).isNotNull();
                    TestdataSpringSolution problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .collect(Collectors.toList()));
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .collect(Collectors.toList()));
                    SolverScope<TestdataSpringSolution> customScope = new SolverScope<>() {
                        @Override
                        public long calculateTimeMillisSpentUpToNow() {
                            // Return five seconds to make the time gradient predictable
                            return 5000L;
                        }
                    };
                    // We ensure the best-score limit won't take priority
                    customScope.setStartingInitializedScore(HardSoftScore.of(-1, -1));
                    customScope.setBestScore(HardSoftScore.of(-1, -1));
                    double gradientTimeDefaultSolver1 =
                            ((DefaultSolverJob<TestdataSpringSolution, Long>) solver1.solve(1L, problem)).getSolverTermination()
                                    .calculateSolverTimeGradient(customScope);
                    assertThat(gradientTimeDefaultSolver1).isEqualTo(0.5);
                    double gradientTimeSolver2 =
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
                    SolverManager<TestdataSpringSolution, Long> solver1 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver1");
                    SolverManager<TestdataSpringSolution, Long> solver2 =
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
                    SolverManager<TestdataSpringSolution, Long> solver1 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver1");
                    SolverManager<TestdataSpringSolution, Long> solver2 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver2");
                    assertThat(solver1).isNotNull();
                    assertThat(solver2).isNotNull();
                });
        gizmoContextRunner
                .withPropertyValues("timefold.solver.solver1.domain-access-type=GIZMO")
                .withPropertyValues("timefold.solver.solver2.domain-access-type=REFLECTION")
                .run(context -> {
                    SolverManager<TestdataSpringSolution, Long> solver1 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver1");
                    SolverManager<TestdataSpringSolution, Long> solver2 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver2");
                    assertThat(solver1).isNotNull();
                    assertThat(solver2).isNotNull();
                });
        contextRunner
                .withPropertyValues("timefold.solver.solver1.daemon=true")
                .withPropertyValues("timefold.solver.solver2.daemon=false")
                .run(context -> {
                    SolverManager<TestdataSpringSolution, Long> solver1 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver1");
                    SolverManager<TestdataSpringSolution, Long> solver2 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver2");
                    assertThat(solver1).isNotNull();
                    assertThat(solver2).isNotNull();
                });
        contextRunner
                .withPropertyValues("timefold.solver.solver1.constraint-stream-impl-type=BAVET")
                .withPropertyValues("timefold.solver.solver2.constraint-stream-impl-type=DROOLS")
                .run(context -> {
                    SolverManager<TestdataSpringSolution, Long> solver1 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver1");
                    SolverManager<TestdataSpringSolution, Long> solver2 =
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
                    SolverConfig solverConfig = context.getBean(SolverConfig.class);
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
                        "timefold.solver.solver2.nearby-distance-meter-class=ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringSolution")
                .run(context -> {
                    SolverConfig solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getNearbyDistanceMeterClass()).isNotNull();
                }))
                .rootCause().message().contains("The Nearby Selection Meter class",
                        "ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringSolution");
    }

    @Test
    void solverPropertiesWithoutLoadingBenchmark() {
        contextRunner
                .withPropertyValues("timefold.solver.solver1.environment-mode=FULL_ASSERT")
                .withPropertyValues("timefold.solver.solver2.environment-mode=TRACKED_FULL_ASSERT")
                .withUserConfiguration(TimefoldBenchmarkAutoConfiguration.class) // We load the configuration, but get no bean
                .run(context -> {
                    SolverManager<TestdataSpringSolution, Long> solver1 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver1");
                    SolverManager<TestdataSpringSolution, Long> solver2 =
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
                    TestdataSpringSolution problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .collect(Collectors.toList()));
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .collect(Collectors.toList()));

                    for (String solverName : List.of("solver1", "solver2")) {
                        SolverManager<TestdataSpringSolution, Long> solver =
                                (SolverManager<TestdataSpringSolution, Long>) context.getBean(solverName);
                        SolverJob<TestdataSpringSolution, Long> solverJob = solver.solve(1L, problem);
                        TestdataSpringSolution solution = solverJob.getFinalBestSolution();
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
                    TestdataSpringSolution problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .collect(Collectors.toList()));
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .collect(Collectors.toList()));

                    for (String solverName : List.of("solver1", "solver2")) {
                        SolverManager<TestdataSpringSolution, Long> solver =
                                (SolverManager<TestdataSpringSolution, Long>) context.getBean(solverName);
                        SolverJob<TestdataSpringSolution, Long> solverJob = solver.solve(1L, problem);
                        TestdataSpringSolution solution = solverJob.getFinalBestSolution();
                        assertThat(solution).isNotNull();
                        assertThat(solution.getScore().score()).isNotNegative();
                    }
                });
    }

    @Test
    void invalidYaml() {
        assertThatCode(() -> contextRunner
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withSystemProperties(
                        "spring.config.location=classpath:ai/timefold/solver/spring/boot/autoconfigure/multiple-solvers/invalid-application.yaml")
                .run(context -> context.getBean(SolverConfig.class)))
                .rootCause().message().contains("The properties", "solverConfigXml", "environmentMode", "moveThreadCount",
                        "domainAccessType", "are not valid", "Maybe try changing the property name to kebab-case");
    }

    @Test
    void invalidTerminationYaml() {
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
                    TestdataSpringSolution problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .collect(Collectors.toList()));
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .collect(Collectors.toList()));
                    for (String solverName : List.of("solver1", "solver2")) {
                        SolverManager<TestdataSpringSolution, Long> solverManager =
                                (SolverManager<TestdataSpringSolution, Long>) context.getBean(solverName);
                        DefaultSolverJob<TestdataSpringSolution, Long> solverJob =
                                (DefaultSolverJob<TestdataSpringSolution, Long>) solverManager.solveBuilder()
                                        .withProblemId(1L)
                                        .withProblem(problem)
                                        .withConfigOverride(
                                                new SolverConfigOverride<TestdataSpringSolution>()
                                                        .withTerminationConfig(new TerminationConfig()
                                                                .withSpentLimit(Duration.ofSeconds(10L))))
                                        .run();
                        SolverScope<TestdataSpringSolution> customScope = new SolverScope<>() {
                            @Override
                            public long calculateTimeMillisSpentUpToNow() {
                                // Return five seconds to make the time gradient predictable
                                return 5000L;
                            }
                        };
                        // We ensure the best-score limit won't take priority
                        customScope.setStartingInitializedScore(HardSoftScore.of(-1, -1));
                        customScope.setBestScore(HardSoftScore.of(-1, -1));
                        double gradientTime = solverJob.getSolverTermination().calculateSolverTimeGradient(customScope);
                        TestdataSpringSolution solution = solverJob.getFinalBestSolution();
                        assertThat(solution).isNotNull();
                        assertThat(solution.getScore().score()).isNotNegative();
                        // Spent-time is 30s by default, but it is overridden with 10. The gradient time must be 50%
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
                    for (String solverName : List.of("solver1", "solver2")) {
                        SolverManager<TestdataSpringSolution, Long> solverManager =
                                (SolverManager<TestdataSpringSolution, Long>) context.getBean(solverName);
                        TestdataSpringSolution problem = new TestdataSpringSolution();
                        problem.setValueList(IntStream.range(1, 3)
                                .mapToObj(i -> "v" + i)
                                .collect(Collectors.toList()));
                        problem.setEntityList(IntStream.range(1, 3)
                                .mapToObj(i -> new TestdataSpringEntity())
                                .collect(Collectors.toList()));
                        SolverJob<TestdataSpringSolution, Long> solverJob = solverManager.solve(1L, problem);
                        TestdataSpringSolution solution = solverJob.getFinalBestSolution();
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
        assertThatCode(() -> benchmarkContextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .withPropertyValues("timefold.benchmark.solver.termination.spent-limit=1s")
                .run(context -> {
                    context.getBean(PlannerBenchmarkFactory.class);
                }))
                .hasRootCauseMessage("""
                        When defining multiple solvers, the benchmark feature is not enabled.
                        Consider using separate <solverBenchmark> instances for evaluating different solver configurations.""");
    }

    @Test
    void chained_solverConfigXml_none() {
        chainedContextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> {
                    SolverManager<TestdataSpringSolution, Long> solver1 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver1");
                    SolverManager<TestdataSpringSolution, Long> solver2 =
                            (SolverManager<TestdataSpringSolution, Long>) context.getBean("solver2");
                    assertThat(solver1).isNotNull();
                    assertThat(solver2).isNotNull();
                });
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
                        TestdataChainedSpringSolution.class.getSimpleName(),
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
                        "Unused classes ([ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringSolution]) found with a @PlanningSolution annotation.");
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
                        DummyChainedSpringEasyScore.class.getSimpleName(),
                        DummySpringEasyScore.class.getSimpleName(),
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
                        TestdataChainedSpringConstraintProvider.class.getSimpleName(),
                        TestdataSpringConstraintProvider.class.getSimpleName(),
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
                        DummyChainedSpringIncrementalScore.class.getSimpleName(),
                        DummySpringIncrementalScore.class.getSimpleName(),
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
                        DummyChainedSpringEasyScore.class.getSimpleName(),
                        DummySpringEasyScore.class.getSimpleName(),
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
                        TestdataChainedSpringConstraintProvider.class.getSimpleName(),
                        TestdataSpringConstraintProvider.class.getSimpleName(),
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
                        DummyChainedSpringIncrementalScore.class.getSimpleName(),
                        DummySpringIncrementalScore.class.getSimpleName(),
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
                        "Unused classes ([" + DummySpringEasyScore.class.getCanonicalName()
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
                        "Unused classes ([" + TestdataChainedSpringConstraintProvider.class.getCanonicalName()
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
                        "Unused classes ([" + DummySpringIncrementalScore.class.getCanonicalName()
                                + "]) that implements IncrementalScoreCalculator were found.");
    }

    @Test
    void invalidEntity() {
        assertThatCode(() -> contextRunner
                .withUserConfiguration(InvalidEntityTypeSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "The classes",
                        "InvalidMethodTestdataSpringEntity",
                        "InvalidFieldTestdataSpringEntity",
                        "do not have the PlanningEntity annotation, even though they contain properties reserved for planning entities.",
                        "Maybe add a @PlanningEntity annotation on the classes");

        assertThatCode(() -> contextRunner
                .withUserConfiguration(InvalidEntitySpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains(
                        "All classes",
                        "InvalidRecordTestdataSpringEntity",
                        "InvalidEnumTestdataSpringEntity",
                        "annotated with @PlanningEntity must be a class");
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
                .cause().message().contains(
                        "All classes",
                        "InvalidRecordTestdataSpringSolution",
                        "annotated with @PlanningSolution must be a class");
    }
}
