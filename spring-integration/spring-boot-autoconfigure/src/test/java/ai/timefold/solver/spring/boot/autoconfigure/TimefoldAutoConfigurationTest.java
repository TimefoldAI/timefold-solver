package ai.timefold.solver.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.score.ScoreManager;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverConfigOverride;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.solver.DefaultSolutionManager;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;
import ai.timefold.solver.core.impl.solver.DefaultSolverJob;
import ai.timefold.solver.core.impl.solver.DefaultSolverManager;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.spring.boot.autoconfigure.chained.ChainedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.chained.constraints.TestdataChainedSpringConstraintProvider;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringObject;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringSolution;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;
import ai.timefold.solver.spring.boot.autoconfigure.gizmo.GizmoSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.multimodule.MultiModuleSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.EmptySpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.NoConstraintsSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.NormalSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.constraints.TestdataSpringConstraintProvider;
import ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringSolution;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import ai.timefold.solver.test.impl.score.stream.DefaultConstraintVerifier;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestExecutionListeners;

@TestExecutionListeners
class TimefoldAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner;
    private final ApplicationContextRunner emptyContextRunner;
    private final ApplicationContextRunner benchmarkContextRunner;
    private final ApplicationContextRunner noConstraintsContextRunner;
    private final ApplicationContextRunner chainedContextRunner;
    private final ApplicationContextRunner gizmoContextRunner;
    private final ApplicationContextRunner multimoduleRunner;
    private final FilteredClassLoader allDefaultsFilteredClassLoader;
    private final FilteredClassLoader testFilteredClassLoader;
    private final FilteredClassLoader noGizmoFilteredClassLoader;

    public TimefoldAutoConfigurationTest() {
        contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TimefoldAutoConfiguration.class))
                .withUserConfiguration(NormalSpringTestConfiguration.class);
        emptyContextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TimefoldAutoConfiguration.class))
                .withUserConfiguration(EmptySpringTestConfiguration.class);
        benchmarkContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldAutoConfiguration.class, TimefoldBenchmarkAutoConfiguration.class))
                .withUserConfiguration(NormalSpringTestConfiguration.class);
        noConstraintsContextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TimefoldAutoConfiguration.class))
                .withUserConfiguration(NoConstraintsSpringTestConfiguration.class);
        gizmoContextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TimefoldAutoConfiguration.class))
                .withUserConfiguration(GizmoSpringTestConfiguration.class);
        chainedContextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TimefoldAutoConfiguration.class))
                .withUserConfiguration(ChainedSpringTestConfiguration.class);
        multimoduleRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TimefoldAutoConfiguration.class))
                .withUserConfiguration(MultiModuleSpringTestConfiguration.class);
        allDefaultsFilteredClassLoader =
                new FilteredClassLoader(FilteredClassLoader.PackageFilter.of("ai.timefold.solver.test"),
                        FilteredClassLoader.ClassPathResourceFilter
                                .of(new ClassPathResource(TimefoldProperties.DEFAULT_SOLVER_CONFIG_URL)));
        testFilteredClassLoader =
                new FilteredClassLoader(new ClassPathResource(TimefoldProperties.DEFAULT_SOLVER_CONFIG_URL));
        noGizmoFilteredClassLoader = new FilteredClassLoader(FilteredClassLoader.PackageFilter.of("io.quarkus.gizmo"),
                FilteredClassLoader.ClassPathResourceFilter.of(
                        new ClassPathResource(TimefoldProperties.DEFAULT_SOLVER_CONFIG_URL)));
    }

    @Test
    void noSolutionOrEntityClasses() {
        emptyContextRunner
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNull();
                });
    }

    @Test
    void solverConfigXml_none() {
        contextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .run(context -> {
                    SolverConfig solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getSolutionClass()).isEqualTo(TestdataSpringSolution.class);
                    assertThat(solverConfig.getEntityClassList())
                            .isEqualTo(Collections.singletonList(TestdataSpringEntity.class));
                    assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                            .isEqualTo(TestdataSpringConstraintProvider.class);
                    // No termination defined
                    assertThat(solverConfig.getTerminationConfig()).isNull();
                    SolverFactory<TestdataSpringSolution> solverFactory = context.getBean(SolverFactory.class);
                    assertThat(solverFactory).isNotNull();
                    assertThat(solverFactory.buildSolver()).isNotNull();
                });
    }

    @Test
    void solverConfigXml_default() {
        contextRunner
                .run(context -> {
                    SolverConfig solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getSolutionClass()).isEqualTo(TestdataSpringSolution.class);
                    assertThat(solverConfig.getEntityClassList())
                            .isEqualTo(Collections.singletonList(TestdataSpringEntity.class));
                    assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                            .isEqualTo(TestdataSpringConstraintProvider.class);
                    // Properties defined in solverConfig.xml
                    assertThat(solverConfig.getTerminationConfig().getSecondsSpentLimit().longValue()).isEqualTo(2L);
                    SolverFactory<TestdataSpringSolution> solverFactory = context.getBean(SolverFactory.class);
                    assertThat(solverFactory).isNotNull();
                    assertThat(solverFactory.buildSolver()).isNotNull();
                });
    }

    @Test
    void solverConfigXml_property() {
        contextRunner
                .withPropertyValues(
                        "timefold.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/customSpringBootSolverConfig.xml")
                .run(context -> {
                    SolverConfig solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getSolutionClass()).isEqualTo(TestdataSpringSolution.class);
                    assertThat(solverConfig.getEntityClassList())
                            .isEqualTo(Collections.singletonList(TestdataSpringEntity.class));
                    assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                            .isEqualTo(TestdataSpringConstraintProvider.class);
                    // Properties defined in customSpringBootSolverConfig.xml
                    assertThat(solverConfig.getTerminationConfig().getMinutesSpentLimit().longValue()).isEqualTo(3L);
                    SolverFactory<TestdataSpringSolution> solverFactory = context.getBean(SolverFactory.class);
                    assertThat(solverFactory).isNotNull();
                    assertThat(solverFactory.buildSolver()).isNotNull();
                });
    }

    @Test
    void solverConfigXml_property_noGlobalTermination() {
        benchmarkContextRunner
                .withPropertyValues(
                        "timefold.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/solverConfigWithoutGlobalTermination.xml")
                .run(context -> {
                    SolverConfig solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getSolutionClass()).isEqualTo(TestdataSpringSolution.class);
                    assertThat(solverConfig.getEntityClassList())
                            .isEqualTo(Collections.singletonList(TestdataSpringEntity.class));
                    assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                            .isEqualTo(TestdataSpringConstraintProvider.class);
                    // Properties defined in customSpringBootSolverConfig.xml
                    SolverFactory<TestdataSpringSolution> solverFactory = context.getBean(SolverFactory.class);
                    assertThat(solverFactory).isNotNull();
                    assertThat(solverFactory.buildSolver()).isNotNull();
                });
    }

    @Test
    void solverProperties() {
        contextRunner
                .withPropertyValues("timefold.solver.environment-mode=FULL_ASSERT")
                .run(context -> {
                    SolverConfig solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig.getEnvironmentMode()).isEqualTo(EnvironmentMode.FULL_ASSERT);
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
        gizmoContextRunner
                .withPropertyValues("timefold.solver.domain-access-type=GIZMO")
                .run(context -> {
                    SolverConfig solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig.getDomainAccessType()).isEqualTo(DomainAccessType.GIZMO);
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
        contextRunner
                .withPropertyValues("timefold.solver.daemon=true")
                .run(context -> {
                    SolverConfig solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig.getDaemon()).isTrue();
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
    }

    @Test
    void terminationProperties() {
        contextRunner
                .withPropertyValues("timefold.solver.termination.spent-limit=4h")
                .run(context -> {
                    TerminationConfig terminationConfig = context.getBean(SolverConfig.class).getTerminationConfig();
                    assertThat(terminationConfig.getSpentLimit()).isEqualTo(Duration.ofHours(4));
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
        contextRunner
                .withPropertyValues("timefold.solver.termination.unimproved-spent-limit=5h")
                .run(context -> {
                    TerminationConfig terminationConfig = context.getBean(SolverConfig.class).getTerminationConfig();
                    assertThat(terminationConfig.getUnimprovedSpentLimit()).isEqualTo(Duration.ofHours(5));
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
        contextRunner
                .withPropertyValues("timefold.solver.termination.best-score-limit=6")
                .run(context -> {
                    TerminationConfig terminationConfig = context.getBean(SolverConfig.class).getTerminationConfig();
                    assertThat(terminationConfig.getBestScoreLimit()).isEqualTo(SimpleScore.of(6).toString());
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
    }

    @Test
    void singletonSolverFactory() {
        contextRunner
                .run(context -> {
                    SolverFactory<TestdataSpringSolution> solverFactory = context.getBean(SolverFactory.class);
                    assertThat(solverFactory).isNotNull();
                    ScoreManager<TestdataSpringSolution, SimpleScore> scoreManager = context.getBean(ScoreManager.class);
                    assertThat(scoreManager).isNotNull();
                    SolutionManager<TestdataSpringSolution, SimpleScore> solutionManager =
                            context.getBean(SolutionManager.class);
                    assertThat(((DefaultSolverFactory) solverFactory).getScoreDirectorFactory())
                            .isSameAs(((DefaultSolutionManager<TestdataSpringSolution, SimpleScore>) solutionManager)
                                    .getScoreDirectorFactory());
                    SolverManager<TestdataSpringSolution, Long> solverManager = context.getBean(SolverManager.class);
                    assertThat(solverManager).isNotNull();
                    // There is only one SolverFactory instance
                    assertThat(((DefaultSolverManager<TestdataSpringSolution, Long>) solverManager).getSolverFactory())
                            .isSameAs(solverFactory);
                });
    }

    @Test
    void solve() {
        contextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0")
                .run(context -> {
                    SolverManager<TestdataSpringSolution, Long> solverManager = context.getBean(SolverManager.class);
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
                    assertThat(solution.getScore().score()).isGreaterThanOrEqualTo(0);
                });
    }

    @Test
    void solveWithTimeOverride() {
        contextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0",
                        "timefold.solver.termination.spent-limit=30s")
                .run(context -> {
                    SolverManager<TestdataSpringSolution, Long> solverManager = context.getBean(SolverManager.class);
                    TestdataSpringSolution problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .collect(Collectors.toList()));
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .collect(Collectors.toList()));
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
                    assertThat(solution.getScore().score()).isGreaterThanOrEqualTo(0);
                    // Spent-time is 30s by default, but it is overridden with 10. The gradient time must be 50%
                    assertThat(gradientTime).isEqualTo(0.5);
                });
    }

    @Test
    void multimoduleSolve() {
        multimoduleRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0")
                .run(context -> {
                    SolverManager<TestdataSpringSolution, Long> solverManager = context.getBean(SolverManager.class);
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
                    assertThat(solution.getScore().score()).isGreaterThanOrEqualTo(0);
                });
    }

    @Test
    void benchmark() {
        benchmarkContextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0")
                .run(context -> {
                    PlannerBenchmarkFactory benchmarkFactory = context.getBean(PlannerBenchmarkFactory.class);
                    TestdataSpringSolution problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .collect(Collectors.toList()));
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .collect(Collectors.toList()));
                    benchmarkFactory.buildPlannerBenchmark(problem).benchmark();
                });
    }

    @Test
    void constraintVerifier() {
        contextRunner
                .withClassLoader(testFilteredClassLoader)
                .run(context -> {
                    ConstraintVerifier<TestdataSpringConstraintProvider, TestdataSpringSolution> constraintVerifier =
                            context.getBean(ConstraintVerifier.class);

                    TestdataSpringSolution problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .collect(Collectors.toList()));
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .collect(Collectors.toList()));

                    problem.getEntityList().get(0).setValue("v1");
                    problem.getEntityList().get(1).setValue("v1");
                    constraintVerifier.verifyThat().givenSolution(problem).scores(SimpleScore.of(-2));

                    problem.getEntityList().get(1).setValue("v2");
                    constraintVerifier.verifyThat().givenSolution(problem).scores(SimpleScore.of(0));
                });
    }

    @Test
    void constraintVerifierBavet() {
        contextRunner
                .withClassLoader(testFilteredClassLoader)
                .withPropertyValues(
                        "timefold.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/bavetSolverConfig.xml")
                .run(context -> {
                    ConstraintVerifier<TestdataSpringConstraintProvider, TestdataSpringSolution> constraintVerifier =
                            context.getBean(ConstraintVerifier.class);
                    assertThat(((DefaultConstraintVerifier) constraintVerifier).getConstraintStreamImplType())
                            .isEqualTo(ConstraintStreamImplType.BAVET);

                    TestdataSpringSolution problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .collect(Collectors.toList()));
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .collect(Collectors.toList()));

                    problem.getEntityList().get(0).setValue("v1");
                    problem.getEntityList().get(1).setValue("v1");
                    constraintVerifier.verifyThat().givenSolution(problem).scores(SimpleScore.of(-2));

                    problem.getEntityList().get(1).setValue("v2");
                    constraintVerifier.verifyThat().givenSolution(problem).scores(SimpleScore.of(0));
                });
    }

    @Test
    void chained_solverConfigXml_none() {
        chainedContextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .run(context -> {
                    SolverConfig solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getSolutionClass()).isEqualTo(TestdataChainedSpringSolution.class);
                    assertThat(solverConfig.getEntityClassList()).containsExactlyInAnyOrder(
                            TestdataChainedSpringObject.class,
                            TestdataChainedSpringEntity.class);
                    assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                            .isEqualTo(TestdataChainedSpringConstraintProvider.class);
                    // No termination defined
                    assertThat(solverConfig.getTerminationConfig()).isNull();
                    SolverFactory<TestdataSpringSolution> solverFactory = context.getBean(SolverFactory.class);
                    assertThat(solverFactory).isNotNull();
                    assertThat(solverFactory.buildSolver()).isNotNull();
                });
    }

    @Test
    @Disabled("Test works when run by itself, but errors when run in suite;" +
            " it appears it still find the class when run in a suite, but not alone.")
    void gizmo_throws_if_gizmo_not_present() {
        assertThatCode(() -> {
            gizmoContextRunner
                    .withClassLoader(noGizmoFilteredClassLoader)
                    .withPropertyValues(
                            "timefold.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/gizmoSpringBootSolverConfig.xml")
                    .run(context -> {
                        context.getBean(SolverFactory.class);
                    });
        })
                .hasRootCauseMessage("When using the domainAccessType (" +
                        DomainAccessType.GIZMO +
                        ") the classpath or modulepath must contain io.quarkus.gizmo:gizmo.\n" +
                        "Maybe add a dependency to io.quarkus.gizmo:gizmo.");
    }

    private TimefoldAutoConfiguration mockAutoConfiguration() {
        TimefoldAutoConfiguration autoConfiguration = mock(TimefoldAutoConfiguration.class);
        doCallRealMethod().when(autoConfiguration).applyScoreDirectorFactoryProperties(any());
        return autoConfiguration;
    }
}
