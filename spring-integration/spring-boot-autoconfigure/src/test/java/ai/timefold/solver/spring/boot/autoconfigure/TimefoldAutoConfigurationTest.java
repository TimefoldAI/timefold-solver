package ai.timefold.solver.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.score.ScoreManager;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.solver.DefaultSolutionManager;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;
import ai.timefold.solver.core.impl.solver.DefaultSolverManager;
import ai.timefold.solver.core.impl.testutil.DisabledInProductization;
import ai.timefold.solver.spring.boot.autoconfigure.chained.ChainedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.chained.constraints.TestdataChainedSpringConstraintProvider;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringObject;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringSolution;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;
import ai.timefold.solver.spring.boot.autoconfigure.gizmo.GizmoSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.multimodule.MultiModuleSpringTestConfiguration;
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
import org.springframework.boot.test.autoconfigure.SpringBootDependencyInjectionTestExecutionListener;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestExecutionListeners;

@TestExecutionListeners(listeners = { SpringBootDependencyInjectionTestExecutionListener.class })
class TimefoldAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner;
    private final ApplicationContextRunner benchmarkContextRunner;
    private final ApplicationContextRunner noConstraintsContextRunner;
    private final ApplicationContextRunner chainedContextRunner;
    private final ApplicationContextRunner gizmoContextRunner;
    private final ApplicationContextRunner multimoduleRunner;
    private final FilteredClassLoader allDefaultsFilteredClassLoader;
    private final FilteredClassLoader testFilteredClassLoader;
    private final FilteredClassLoader noGizmoFilteredClassLoader;
    private final FilteredClassLoader defaultConstraintsDrlFilteredClassLoader;

    public TimefoldAutoConfigurationTest() {
        contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TimefoldAutoConfiguration.class))
                .withUserConfiguration(NormalSpringTestConfiguration.class);
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
                new FilteredClassLoader(FilteredClassLoader.PackageFilter.of("org.optaplanner.test"),
                        FilteredClassLoader.ClassPathResourceFilter
                                .of(new ClassPathResource(TimefoldProperties.DEFAULT_SOLVER_CONFIG_URL)),
                        FilteredClassLoader.ClassPathResourceFilter
                                .of(new ClassPathResource(TimefoldProperties.DEFAULT_CONSTRAINTS_DRL_URL)));
        testFilteredClassLoader =
                new FilteredClassLoader(new ClassPathResource(TimefoldProperties.DEFAULT_SOLVER_CONFIG_URL),
                        new ClassPathResource(TimefoldProperties.DEFAULT_CONSTRAINTS_DRL_URL));
        defaultConstraintsDrlFilteredClassLoader =
                new FilteredClassLoader(new ClassPathResource(TimefoldProperties.DEFAULT_CONSTRAINTS_DRL_URL));
        noGizmoFilteredClassLoader = new FilteredClassLoader(FilteredClassLoader.PackageFilter.of("io.quarkus.gizmo"),
                FilteredClassLoader.ClassPathResourceFilter.of(
                        new ClassPathResource(TimefoldProperties.DEFAULT_SOLVER_CONFIG_URL)),
                FilteredClassLoader.ClassPathResourceFilter.of(
                        new ClassPathResource(TimefoldProperties.DEFAULT_CONSTRAINTS_DRL_URL)));
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
                .withClassLoader(defaultConstraintsDrlFilteredClassLoader)
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
                .withClassLoader(defaultConstraintsDrlFilteredClassLoader)
                .withPropertyValues(
                        "optaplanner.solver-config-xml=org/optaplanner/spring/boot/autoconfigure/customSpringBootSolverConfig.xml")
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
    void solverProperties() {
        contextRunner
                .withClassLoader(defaultConstraintsDrlFilteredClassLoader)
                .withPropertyValues("optaplanner.solver.environment-mode=FULL_ASSERT")
                .run(context -> {
                    SolverConfig solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig.getEnvironmentMode()).isEqualTo(EnvironmentMode.FULL_ASSERT);
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
        gizmoContextRunner
                .withPropertyValues("optaplanner.solver.domain-access-type=GIZMO")
                .run(context -> {
                    SolverConfig solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig.getDomainAccessType()).isEqualTo(DomainAccessType.GIZMO);
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
        contextRunner
                .withClassLoader(defaultConstraintsDrlFilteredClassLoader)
                .withPropertyValues("optaplanner.solver.daemon=true")
                .run(context -> {
                    SolverConfig solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig.getDaemon()).isTrue();
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
        contextRunner
                .withClassLoader(defaultConstraintsDrlFilteredClassLoader)
                .withPropertyValues("optaplanner.solver.move-thread-count=2")
                .run(context -> {
                    SolverConfig solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig.getMoveThreadCount()).isEqualTo("2");
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
    }

    @DisabledInProductization
    @Test
    void solverPropertiesBavet() {
        contextRunner
                .withClassLoader(defaultConstraintsDrlFilteredClassLoader)
                .withPropertyValues("optaplanner.solver.constraint-stream-impl-type=BAVET")
                .run(context -> {
                    SolverConfig solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintStreamImplType())
                            .isEqualTo(ConstraintStreamImplType.BAVET);
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
    }

    @Test
    void terminationProperties() {
        contextRunner
                .withClassLoader(defaultConstraintsDrlFilteredClassLoader)
                .withPropertyValues("optaplanner.solver.termination.spent-limit=4h")
                .run(context -> {
                    TerminationConfig terminationConfig = context.getBean(SolverConfig.class).getTerminationConfig();
                    assertThat(terminationConfig.getSpentLimit()).isEqualTo(Duration.ofHours(4));
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
        contextRunner
                .withClassLoader(defaultConstraintsDrlFilteredClassLoader)
                .withPropertyValues("optaplanner.solver.termination.unimproved-spent-limit=5h")
                .run(context -> {
                    TerminationConfig terminationConfig = context.getBean(SolverConfig.class).getTerminationConfig();
                    assertThat(terminationConfig.getUnimprovedSpentLimit()).isEqualTo(Duration.ofHours(5));
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
        contextRunner
                .withClassLoader(defaultConstraintsDrlFilteredClassLoader)
                .withPropertyValues("optaplanner.solver.termination.best-score-limit=6")
                .run(context -> {
                    TerminationConfig terminationConfig = context.getBean(SolverConfig.class).getTerminationConfig();
                    assertThat(terminationConfig.getBestScoreLimit()).isEqualTo(SimpleScore.of(6).toString());
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
    }

    @Test
    void singletonSolverFactory() {
        contextRunner
                .withClassLoader(defaultConstraintsDrlFilteredClassLoader)
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
                .withPropertyValues("optaplanner.solver.termination.best-score-limit=0")
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
    void multimoduleSolve() {
        multimoduleRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("optaplanner.solver.termination.best-score-limit=0")
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
                .withPropertyValues("optaplanner.solver.termination.best-score-limit=0")
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
    void constraintVerifierDrools() {
        contextRunner
                .withClassLoader(testFilteredClassLoader)
                .withPropertyValues(
                        "optaplanner.solver-config-xml=org/optaplanner/spring/boot/autoconfigure/droolsSolverConfig.xml")
                .run(context -> {
                    ConstraintVerifier<TestdataSpringConstraintProvider, TestdataSpringSolution> constraintVerifier =
                            context.getBean(ConstraintVerifier.class);

                    assertThat(((DefaultConstraintVerifier) constraintVerifier).getConstraintStreamImplType())
                            .isEqualTo(ConstraintStreamImplType.DROOLS);
                    assertThat(((DefaultConstraintVerifier) constraintVerifier).isDroolsAlphaNetworkCompilationEnabled())
                            .isFalse();
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

    @DisabledInProductization
    @Test
    void constraintVerifierBavet() {
        contextRunner
                .withClassLoader(testFilteredClassLoader)
                .withPropertyValues(
                        "optaplanner.solver-config-xml=org/optaplanner/spring/boot/autoconfigure/bavetSolverConfig.xml")
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
    void constraintVerifierOnDrl() {
        String constraintsUrl = "org/optaplanner/spring/boot/autoconfigure/customConstraints.drl";
        noConstraintsContextRunner
                .withPropertyValues(TimefoldProperties.SCORE_DRL_PROPERTY + "=" + constraintsUrl)
                .withClassLoader(testFilteredClassLoader)
                .run(context -> {
                    assertThatCode(() -> {
                        context.getBean(ConstraintVerifier.class).verifyThat();
                    })
                            .hasMessage("Cannot provision a ConstraintVerifier because there is no ConstraintProvider class.");
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
    void constraintsDrlProperty() {
        String constraintsUrl = "org/optaplanner/spring/boot/autoconfigure/customConstraints.drl";
        noConstraintsContextRunner
                .withPropertyValues(TimefoldProperties.SCORE_DRL_PROPERTY + "=" + constraintsUrl)
                .run(context -> {
                    SolverConfig solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getScoreDirectorFactoryConfig().getScoreDrlList())
                            .containsExactly(constraintsUrl);
                });
    }

    @Test
    void constraintsDrlDefault() {
        noConstraintsContextRunner
                .run(context -> {
                    SolverConfig solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getScoreDirectorFactoryConfig().getScoreDrlList())
                            .containsExactly("constraints.drl");
                });
    }

    @Test
    void constraintsDrlProperty_conflictWithConstraintProvider() {
        String constraintsUrl = "org/optaplanner/spring/boot/autoconfigure/customConstraints.drl";
        contextRunner
                .withPropertyValues(TimefoldProperties.SCORE_DRL_PROPERTY + "=" + constraintsUrl)
                .run(context -> {
                    assertThatExceptionOfType(IllegalStateException.class)
                            .isThrownBy(() -> context.getBean(SolverConfig.class))
                            .withStackTraceContaining("The scoreDirectorFactory cannot have a constraintProviderClass ("
                                    + TestdataSpringConstraintProvider.class.getName() + ") and a scoreDrlList ("
                                    + constraintsUrl + ")");
                });
    }

    @Test
    void customScoreDrl_overrides_solverConfig() {
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withScoreDrls("config_constraints.drl");
        SolverConfig solverConfig = new SolverConfig().withScoreDirectorFactory(scoreDirectorFactoryConfig);
        TimefoldAutoConfiguration autoConfiguration = mockAutoConfiguration();
        when(autoConfiguration.constraintsDrl()).thenReturn("some.drl");

        autoConfiguration.applyScoreDirectorFactoryProperties(solverConfig);
        assertThat(scoreDirectorFactoryConfig.getScoreDrlList()).containsExactly("some.drl");
    }

    @Test
    void defaultScoreDrl_does_not_override_solverConfig() {
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withScoreDrls("config_constraints.drl");
        SolverConfig solverConfig = new SolverConfig().withScoreDirectorFactory(scoreDirectorFactoryConfig);
        TimefoldAutoConfiguration autoConfiguration = mockAutoConfiguration();
        when(autoConfiguration.constraintsDrl()).thenReturn(null);
        when(autoConfiguration.defaultConstraintsDrl())
                .thenReturn(TimefoldProperties.DEFAULT_CONSTRAINTS_DRL_URL);

        autoConfiguration.applyScoreDirectorFactoryProperties(solverConfig);
        assertThat(scoreDirectorFactoryConfig.getScoreDrlList())
                .containsExactly("config_constraints.drl");
    }

    @Test
    void defaultScoreDrl_applies_if_solverConfig_does_not_define_scoreDrl() {
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig();
        SolverConfig solverConfig = new SolverConfig().withScoreDirectorFactory(scoreDirectorFactoryConfig);
        TimefoldAutoConfiguration autoConfiguration = mockAutoConfiguration();
        when(autoConfiguration.constraintsDrl()).thenReturn(null);
        when(autoConfiguration.defaultConstraintsDrl())
                .thenReturn(TimefoldProperties.DEFAULT_CONSTRAINTS_DRL_URL);

        autoConfiguration.applyScoreDirectorFactoryProperties(solverConfig);
        assertThat(scoreDirectorFactoryConfig.getScoreDrlList())
                .containsExactly(TimefoldProperties.DEFAULT_CONSTRAINTS_DRL_URL);
    }

    @Test
    @Disabled("Test works when run by itself, but errors when run in suite;" +
            " it appears it still find the class when run in a suite, but not alone.")
    void gizmo_throws_if_gizmo_not_present() {
        assertThatCode(() -> {
            gizmoContextRunner
                    .withClassLoader(noGizmoFilteredClassLoader)
                    .withPropertyValues(
                            "optaplanner.solver-config-xml=org/optaplanner/spring/boot/autoconfigure/gizmoSpringBootSolverConfig.xml")
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
