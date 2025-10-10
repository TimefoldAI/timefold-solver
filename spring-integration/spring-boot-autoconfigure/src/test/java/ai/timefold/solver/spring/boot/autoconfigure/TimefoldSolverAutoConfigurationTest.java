package ai.timefold.solver.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.score.ScoreManager;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverConfigOverride;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.DiminishedReturnsTerminationConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.solver.DefaultSolutionManager;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;
import ai.timefold.solver.core.impl.solver.DefaultSolverJob;
import ai.timefold.solver.core.impl.solver.DefaultSolverManager;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.spring.boot.autoconfigure.chained.ChainedSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.chained.constraints.TestdataChainedSpringConstraintProvider;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringAnchor;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringObject;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringSolution;
import ai.timefold.solver.spring.boot.autoconfigure.config.SolverProperty;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;
import ai.timefold.solver.spring.boot.autoconfigure.declarative.SupplierVariableSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.declarative.domain.TestdataSpringSupplierVariableEntity;
import ai.timefold.solver.spring.boot.autoconfigure.declarative.domain.TestdataSpringSupplierVariableSolution;
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
import ai.timefold.solver.spring.boot.autoconfigure.missingsuppliervariable.MissingSupplierVariableSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.multimodule.MultiModuleSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.multiple.MultipleConstraintSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.EmptySpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.NoConstraintsSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.NormalSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.normal.constraints.TestdataSpringConstraintProvider;
import ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringSolution;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.NativeDetector;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestExecutionListeners;

@TestExecutionListeners
@Execution(ExecutionMode.CONCURRENT)
class TimefoldSolverAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner;
    private final ApplicationContextRunner emptyContextRunner;
    private final ApplicationContextRunner fakeNativeWithNodeSharingContextRunner;
    private final ApplicationContextRunner fakeNativeWithoutNodeSharingContextRunner;
    private final ApplicationContextRunner benchmarkContextRunner;
    private final ApplicationContextRunner noUserConfigurationContextRunner;
    private final ApplicationContextRunner chainedContextRunner;
    private final ApplicationContextRunner supplierVariableContextRunner;
    private final ApplicationContextRunner missingSupplierVariableContextRunner;
    private final ApplicationContextRunner gizmoContextRunner;
    private final ApplicationContextRunner multimoduleRunner;
    private final ApplicationContextRunner multiConstraintProviderRunner;
    private final FilteredClassLoader allDefaultsFilteredClassLoader;
    private final FilteredClassLoader testFilteredClassLoader;
    private final FilteredClassLoader noGizmoFilteredClassLoader;

    public TimefoldSolverAutoConfigurationTest() {
        contextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(NormalSpringTestConfiguration.class);
        emptyContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(EmptySpringTestConfiguration.class);
        fakeNativeWithNodeSharingContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(NormalSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.%s=true"
                        .formatted(SolverProperty.CONSTRAINT_STREAM_AUTOMATIC_NODE_SHARING.getPropertyName()));
        fakeNativeWithoutNodeSharingContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(NormalSpringTestConfiguration.class)
                .withPropertyValues("timefold.solver.%s=false"
                        .formatted(SolverProperty.CONSTRAINT_STREAM_AUTOMATIC_NODE_SHARING.getPropertyName()));
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
        supplierVariableContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(SupplierVariableSpringTestConfiguration.class);
        missingSupplierVariableContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MissingSupplierVariableSpringTestConfiguration.class);
        multimoduleRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MultiModuleSpringTestConfiguration.class);
        multiConstraintProviderRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(MultipleConstraintSpringTestConfiguration.class);
        allDefaultsFilteredClassLoader =
                new FilteredClassLoader(FilteredClassLoader.PackageFilter.of("ai.timefold.solver.test"),
                        FilteredClassLoader.ClassPathResourceFilter
                                .of(new ClassPathResource(TimefoldProperties.DEFAULT_SOLVER_CONFIG_URL)));
        testFilteredClassLoader =
                new FilteredClassLoader(new ClassPathResource(TimefoldProperties.DEFAULT_SOLVER_CONFIG_URL));
        noGizmoFilteredClassLoader = new FilteredClassLoader(FilteredClassLoader.PackageFilter.of("io.quarkus.gizmo"),
                FilteredClassLoader.ClassPathResourceFilter.of(
                        new ClassPathResource(TimefoldProperties.DEFAULT_SOLVER_CONFIG_URL)));
        noUserConfigurationContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class));
    }

    @Test
    void noSolutionOrEntityClasses() {
        emptyContextRunner
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNull();
                });
    }

    @Test
    void nodeSharingFailFastInNativeImage() {
        try (var nativeDetectorMock = Mockito.mockStatic(NativeDetector.class)) {
            nativeDetectorMock.when(NativeDetector::inNativeImage).thenReturn(true);
            fakeNativeWithNodeSharingContextRunner
                    .run(context -> {
                        Throwable startupFailure = context.getStartupFailure();
                        assertThat(startupFailure)
                                .isInstanceOf(UnsupportedOperationException.class)
                                .hasMessageContainingAll("node sharing", "unsupported", "native");
                    });
        }

    }

    @Test
    void nodeSharingDisabledWorksInNativeImage() {
        try (var nativeDetectorMock = Mockito.mockStatic(NativeDetector.class)) {
            nativeDetectorMock.when(NativeDetector::inNativeImage).thenReturn(true);
            fakeNativeWithoutNodeSharingContextRunner
                    .run(context -> {
                        var solverConfig = context.getBean(SolverConfig.class);
                        assertThat(solverConfig).isNotNull();
                        assertThat(solverConfig.getSolutionClass()).isEqualTo(TestdataSpringSolution.class);
                        assertThat(solverConfig.getEntityClassList())
                                .isEqualTo(Collections.singletonList(TestdataSpringEntity.class));
                        assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                                .isEqualTo(TestdataSpringConstraintProvider.class);
                        // Properties defined in solverConfig.xml
                        assertThat(solverConfig.getTerminationConfig().getSecondsSpentLimit().longValue()).isEqualTo(2L);
                        var solverFactory = context.getBean(SolverFactory.class);
                        assertThat(solverFactory).isNotNull();
                        assertThat(solverFactory.buildSolver()).isNotNull();
                    });
        }
    }

    @Test
    void solverConfigXml_none() {
        contextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getSolutionClass()).isEqualTo(TestdataSpringSolution.class);
                    assertThat(solverConfig.getEntityClassList())
                            .isEqualTo(Collections.singletonList(TestdataSpringEntity.class));
                    assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                            .isEqualTo(TestdataSpringConstraintProvider.class);
                    // No termination defined
                    assertThat(solverConfig.getTerminationConfig()).isNull();
                    var solverFactory = context.getBean(SolverFactory.class);
                    assertThat(solverFactory).isNotNull();
                    assertThat(solverFactory.buildSolver()).isNotNull();
                });
    }

    @Test
    void solverConfigXml_default() {
        contextRunner
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getSolutionClass()).isEqualTo(TestdataSpringSolution.class);
                    assertThat(solverConfig.getEntityClassList())
                            .isEqualTo(Collections.singletonList(TestdataSpringEntity.class));
                    assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                            .isEqualTo(TestdataSpringConstraintProvider.class);
                    // Properties defined in solverConfig.xml
                    assertThat(solverConfig.getTerminationConfig().getSecondsSpentLimit().longValue()).isEqualTo(2L);
                    var solverFactory = context.getBean(SolverFactory.class);
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
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getSolutionClass()).isEqualTo(TestdataSpringSolution.class);
                    assertThat(solverConfig.getEntityClassList())
                            .isEqualTo(Collections.singletonList(TestdataSpringEntity.class));
                    assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                            .isEqualTo(TestdataSpringConstraintProvider.class);
                    // Properties defined in customSpringBootSolverConfig.xml
                    assertThat(solverConfig.getTerminationConfig().getMinutesSpentLimit().longValue()).isEqualTo(3L);
                    var solverFactory = context.getBean(SolverFactory.class);
                    assertThat(solverFactory).isNotNull();
                    assertThat(solverFactory.buildSolver()).isNotNull();
                });
    }

    @Test
    void solverNearbyConfigXml_property() {
        contextRunner
                .withPropertyValues(
                        "timefold.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/nearbySolverConfig.xml")
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getNearbyDistanceMeterClass()).isNotNull();
                });
    }

    @Test
    void solverConfigXml_solverPropertyPrecedence() {
        contextRunner
                .withPropertyValues(
                        "timefold.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/solverConfigWithoutGlobalTermination.xml")
                .withPropertyValues(
                        "timefold.solver.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/customSpringBootSolverConfig.xml")
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getSolutionClass()).isEqualTo(TestdataSpringSolution.class);
                    assertThat(solverConfig.getEntityClassList())
                            .isEqualTo(Collections.singletonList(TestdataSpringEntity.class));
                    assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                            .isEqualTo(TestdataSpringConstraintProvider.class);
                    // Properties defined in customSpringBootSolverConfig.xml
                    assertThat(solverConfig.getTerminationConfig().getMinutesSpentLimit().longValue()).isEqualTo(3L);
                    var solverFactory = context.getBean(SolverFactory.class);
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
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getSolutionClass()).isEqualTo(TestdataSpringSolution.class);
                    assertThat(solverConfig.getEntityClassList())
                            .isEqualTo(Collections.singletonList(TestdataSpringEntity.class));
                    assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                            .isEqualTo(TestdataSpringConstraintProvider.class);
                    // Properties defined in customSpringBootSolverConfig.xml
                    var solverFactory = context.getBean(SolverFactory.class);
                    assertThat(solverFactory).isNotNull();
                    assertThat(solverFactory.buildSolver()).isNotNull();
                });
    }

    @Test
    void solverProperties() {
        contextRunner
                .withPropertyValues("timefold.solver.environment-mode=FULL_ASSERT")
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig.getEnvironmentMode()).isEqualTo(EnvironmentMode.FULL_ASSERT);
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
        gizmoContextRunner
                .withPropertyValues("timefold.solver.domain-access-type=GIZMO")
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig.getDomainAccessType()).isEqualTo(DomainAccessType.GIZMO);
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
        contextRunner
                .withPropertyValues("timefold.solver.daemon=true")
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig.getDaemon()).isTrue();
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                });
        contextRunner
                .withPropertyValues(
                        "timefold.solver.nearby-distance-meter-class=ai.timefold.solver.spring.boot.autoconfigure.dummy.DummyDistanceMeter")
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getNearbyDistanceMeterClass()).isNotNull();
                });
        contextRunner
                .withPropertyValues("timefold.solver.random-seed=123")
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig.getRandomSeed()).isEqualTo(123L);
                });
    }

    @Test
    void invalidNearbyClass() {
        // Class not found
        assertThatCode(() -> contextRunner
                .withPropertyValues(
                        "timefold.solver.nearby-distance-meter-class=ai.timefold.solver.spring.boot.autoconfigure.dummy.BadDummyDistanceMeter")
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getNearbyDistanceMeterClass()).isNotNull();
                }))
                .rootCause().message().contains("Cannot find the Nearby Selection Meter class",
                        "ai.timefold.solver.spring.boot.autoconfigure.dummy.BadDummyDistanceMeter");
        // Invalid class
        assertThatCode(() -> contextRunner
                .withPropertyValues(
                        "timefold.solver.nearby-distance-meter-class=ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringSolution")
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getNearbyDistanceMeterClass()).isNotNull();
                }))
                .rootCause().message().contains("The Nearby Selection Meter class",
                        "ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringSolution");
    }

    @Test
    void solverWithYaml() {
        contextRunner
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withSystemProperties(
                        "spring.config.location=classpath:ai/timefold/solver/spring/boot/autoconfigure/single-solver/application.yaml")
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertNotNull(solverConfig);
                    assertNotNull(solverConfig.getNearbyDistanceMeterClass());
                    assertEquals(EnvironmentMode.FULL_ASSERT, solverConfig.getEnvironmentMode());
                    assertTrue(solverConfig.getDaemon());
                    assertEquals("2", solverConfig.getMoveThreadCount());
                    assertEquals(DomainAccessType.REFLECTION, solverConfig.getDomainAccessType());
                    assertNull(solverConfig.getScoreDirectorFactoryConfig().getConstraintStreamImplType());
                    assertEquals(Duration.ofHours(4), solverConfig.getTerminationConfig().getSpentLimit());
                    assertEquals(Duration.ofHours(5), solverConfig.getTerminationConfig().getUnimprovedSpentLimit());
                    assertEquals(SimpleScore.of(0).toString(), solverConfig.getTerminationConfig().getBestScoreLimit());
                });
    }

    @Test
    void invalidYaml() {
        assertThatCode(() -> contextRunner
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withSystemProperties(
                        "spring.config.location=classpath:ai/timefold/solver/spring/boot/autoconfigure/single-solver/invalid-application.yaml")
                .run(context -> context.getBean(SolverConfig.class)))
                .rootCause().message().contains("Cannot use global solver properties with named solvers", "solverConfigXml",
                        "environmentMode", "moveThreadCount",
                        "domainAccessType", "Expected all values to be maps, but values for key(s)",
                        "Maybe try changing the property name to kebab-case");
    }

    @Test
    void invalidTerminationYaml() {
        assertThatCode(() -> contextRunner
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withSystemProperties(
                        "spring.config.location=classpath:ai/timefold/solver/spring/boot/autoconfigure/single-solver/invalid-termination-application.yaml")
                .run(context -> context.getBean(SolverConfig.class)))
                .rootCause().message().contains("The termination properties", "spentLimit", "unimprovedSpentLimit",
                        "bestScoreLimit", "are not valid", "Maybe try changing the property name to kebab-case");
    }

    @Test
    void solveWithAllResources() {
        contextRunner
                .run(context -> {
                    assertThat(context.getBean(SolverConfig.class)).isNotNull();
                    assertThat(context.getBean(SolverFactory.class)).isNotNull();
                    assertThat(context.getBean(SolverManager.class)).isNotNull();
                    assertThat(context.getBean(SolutionManager.class)).isNotNull();
                    assertThat(context.getBean(ScoreManager.class)).isNotNull();
                    assertThat(context.getBean(ConstraintVerifier.class)).isNotNull();
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
    void diminishedReturnsProperties() {
        contextRunner
                .run(context -> {
                    var phases = context.getBean(SolverConfig.class).getPhaseConfigList();
                    assertThat(phases).isNull();
                });

        contextRunner
                .withPropertyValues("timefold.solver.termination.diminished-returns.enabled=true")
                .run(context -> {
                    var terminationConfig = context.getBean(SolverConfig.class).getTerminationConfig();
                    assertThat(terminationConfig)
                            .extracting(TerminationConfig::getDiminishedReturnsConfig)
                            .isNotNull()
                            .hasAllNullFieldsOrProperties();
                });

        contextRunner
                .withPropertyValues("timefold.solver.termination.diminished-returns.sliding-window-duration=5m")
                .run(context -> {
                    var terminationConfig = context.getBean(SolverConfig.class).getTerminationConfig();
                    assertThat(terminationConfig)
                            .extracting(TerminationConfig::getDiminishedReturnsConfig)
                            .isNotNull()
                            .hasAllNullFieldsOrPropertiesExcept("slidingWindowDuration")
                            .extracting(DiminishedReturnsTerminationConfig::getSlidingWindowDuration)
                            .isEqualTo(Duration.ofMinutes(5));
                });

        contextRunner
                .withPropertyValues("timefold.solver.termination.diminished-returns.minimum-improvement-ratio=2.5")
                .run(context -> {
                    var terminationConfig = context.getBean(SolverConfig.class).getTerminationConfig();
                    assertThat(terminationConfig)
                            .extracting(TerminationConfig::getDiminishedReturnsConfig)
                            .isNotNull()
                            .hasAllNullFieldsOrPropertiesExcept("minimumImprovementRatio")
                            .extracting(DiminishedReturnsTerminationConfig::getMinimumImprovementRatio)
                            .isEqualTo(2.5);
                });

        contextRunner
                .withPropertyValues("timefold.solver.termination.diminished-returns.enabled=false")
                .withPropertyValues("timefold.solver.termination.diminished-returns.minimum-improvement-ratio=2.5")
                .run(context -> {
                    var terminationConfig = context.getBean(SolverConfig.class).getTerminationConfig();
                    assertThat(terminationConfig)
                            .extracting(TerminationConfig::getDiminishedReturnsConfig)
                            .isNull();
                });

        contextRunner
                .withPropertyValues("timefold.solver.termination.diminished-returns.enabled=false")
                .withPropertyValues("timefold.solver.termination.diminished-returns.minimum-improvement-ratio=2.5")
                .withPropertyValues(
                        "timefold.solver-config-xml=ai/timefold/solver/spring/boot/autoconfigure/solverConfigWithPhases.xml")
                .run(context -> {
                    var phases = context.getBean(SolverConfig.class).getPhaseConfigList();
                    assertThat(phases).hasSize(2);
                    assertThat(phases).element(0).isInstanceOf(ConstructionHeuristicPhaseConfig.class)
                            .extracting(PhaseConfig::getTerminationConfig).isNull();
                    assertThat(phases).element(1).isInstanceOf(LocalSearchPhaseConfig.class)
                            .extracting(PhaseConfig::getTerminationConfig).isNull();
                });
    }

    @Test
    void singletonSolverFactory() {
        contextRunner
                .run(context -> {
                    var solverFactory = context.getBean(SolverFactory.class);
                    assertThat(solverFactory).isNotNull();
                    var scoreManager = context.getBean(ScoreManager.class);
                    assertThat(scoreManager).isNotNull();
                    SolutionManager<TestdataSpringSolution, SimpleScore> solutionManager =
                            context.getBean(SolutionManager.class);
                    assertThat(((DefaultSolverFactory) solverFactory).getScoreDirectorFactory())
                            .isSameAs(((DefaultSolutionManager<TestdataSpringSolution, SimpleScore>) solutionManager)
                                    .getScoreDirectorFactory());
                    var solverManager = context.getBean(SolverManager.class);
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
                });
    }

    @Test
    void solveWithParallelSolverCount() {
        contextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver-manager.parallel-solver-count=2")
                .run(context -> {
                    var solverManager = context.getBean(SolverManager.class);
                    assertThat(solverManager).isNotNull();
                });
    }

    @Test
    void solveWithTimeOverride() {
        contextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0",
                        "timefold.solver.termination.spent-limit=30s")
                .run(context -> {
                    var solverManager = context.getBean(SolverManager.class);
                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());
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
                });
    }

    @Test
    void multimoduleSolve() {
        multimoduleRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0")
                .run(context -> {
                    SolverManager<TestdataSpringSolution, Long> solverManager = context.getBean(SolverManager.class);
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
                });
    }

    @Test
    void benchmarkWithSpentLimit() {
        benchmarkContextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.benchmark.solver.termination.spent-limit=1s")
                .run(context -> {
                    var benchmarkFactory = context.getBean(PlannerBenchmarkFactory.class);
                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());
                    assertThat(benchmarkFactory.buildPlannerBenchmark(problem).benchmark()).isNotEmptyDirectory();
                });
    }

    @Test
    void benchmark() {
        benchmarkContextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.solver.termination.best-score-limit=0")
                .run(context -> {
                    var benchmarkFactory = context.getBean(PlannerBenchmarkFactory.class);
                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());
                    assertThat(benchmarkFactory.buildPlannerBenchmark(problem).benchmark()).isNotEmptyDirectory();
                });
    }

    @Test
    void benchmarkWithXml() {
        benchmarkContextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues("timefold.benchmark.solver.termination.spent-limit=100ms")
                .withPropertyValues(
                        "timefold.benchmark.solver-benchmark-config-xml=ai/timefold/solver/spring/boot/autoconfigure/solverBenchmarkConfig.xml")
                .run(context -> {
                    var benchmarkFactory = context.getBean(PlannerBenchmarkFactory.class);
                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());
                    assertThat(benchmarkFactory.buildPlannerBenchmark(problem).benchmark()).isNotEmptyDirectory();
                });
    }

    @Test
    void constraintMetaModel() {
        contextRunner.withClassLoader(testFilteredClassLoader)
                .run(context -> {
                    var constraintMetaModel = context.getBean(ConstraintMetaModel.class);
                    assertThat(constraintMetaModel).isNotNull();
                });
    }

    @Test
    void constraintVerifier() {
        contextRunner
                .withClassLoader(testFilteredClassLoader)
                .run(context -> {
                    ConstraintVerifier<TestdataSpringConstraintProvider, TestdataSpringSolution> constraintVerifier =
                            context.getBean(ConstraintVerifier.class);

                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());

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

                    var problem = new TestdataSpringSolution();
                    problem.setValueList(IntStream.range(1, 3)
                            .mapToObj(i -> "v" + i)
                            .toList());
                    problem.setEntityList(IntStream.range(1, 3)
                            .mapToObj(i -> new TestdataSpringEntity())
                            .toList());

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
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getSolutionClass()).isEqualTo(TestdataChainedSpringSolution.class);
                    assertThat(solverConfig.getEntityClassList()).containsExactlyInAnyOrder(
                            TestdataChainedSpringObject.class,
                            TestdataChainedSpringEntity.class,
                            TestdataChainedSpringAnchor.class);
                    assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                            .isEqualTo(TestdataChainedSpringConstraintProvider.class);
                    // No termination defined
                    assertThat(solverConfig.getTerminationConfig()).isNull();
                    var solverFactory = context.getBean(SolverFactory.class);
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
    void readOnlyConcreteProviderClass() {
        AssertionsForClassTypes.assertThatCode(() -> multiConstraintProviderRunner
                .run(context -> context.getBean(SolverFactory.class)))
                .doesNotThrowAnyException();
    }

    @Test
    void invalidEntity() {
        assertThatCode(() -> contextRunner
                .withUserConfiguration(InvalidEntityTypeSpringTestConfiguration.class)
                .run(context -> context.getBean("solver1")))
                .cause().message().contains("is not annotated with @PlanningEntity but defines genuine or shadow variables.");

        assertThatCode(() -> contextRunner
                .withUserConfiguration(InvalidEntitySpringTestConfiguration.class)
                .run(context -> context.getBean("solver1")))
                .cause().message().contains("it needs to be mutable.");
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

    @Test
    void solveSupplierVariables() {
        supplierVariableContextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues(
                        "timefold.solver.termination.best-score-limit=0")
                .run(context -> {
                    SolverManager<TestdataSpringSupplierVariableSolution, Long> solverManager =
                            context.getBean(SolverManager.class);
                    var problem = new TestdataSpringSupplierVariableSolution();
                    problem.setValueList(List.of("a", "b"));
                    problem.setEntityList(List.of(new TestdataSpringSupplierVariableEntity()));
                    var solverJob = solverManager.solve(1L, problem);
                    var solution = solverJob.getFinalBestSolution();
                    assertThat(solution).isNotNull();
                    assertThat(solution.getScore().score()).isNotNegative();
                });
    }

    @Test
    void missingSupplierVariables() {
        assertThatCode(() -> missingSupplierVariableContextRunner
                .withClassLoader(allDefaultsFilteredClassLoader)
                .withPropertyValues(
                        "timefold.solver.termination.best-score-limit=0")
                .run(context -> {
                    context.getBean(SolverFactory.class);
                })).hasMessageContainingAll("@ShadowVariable (value1AndValue2)",
                        "supplierMethod (value1AndValue2Supplier) that does not exist",
                        "inside its declaring class (ai.timefold.solver.spring.boot.autoconfigure.missingsuppliervariable.domain.TestdataSpringMissingSupplierVariableEntity).",
                        "Maybe you misspelled the supplierMethod name?");
    }
}
