package ai.timefold.solver.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collections;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
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
import ai.timefold.solver.core.impl.solver.DefaultSolverManager;
import ai.timefold.solver.spring.boot.autoconfigure.basic.EmptySpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.basic.NormalSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.basic.constraints.TestdataSpringConstraintProvider;
import ai.timefold.solver.spring.boot.autoconfigure.basic.domain.TestdataSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.basic.domain.TestdataSpringSolution;
import ai.timefold.solver.spring.boot.autoconfigure.invalid.entity.InvalidEntitySpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.invalid.type.InvalidEntityTypeSpringTestConfiguration;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.TestExecutionListeners;

@TestExecutionListeners
@Execution(ExecutionMode.CONCURRENT)
@ResourceLock("yamlAndXml")
class TimefoldSolverWithSolverConfigXmlAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner;
    private final ApplicationContextRunner emptyContextRunner;

    public TimefoldSolverWithSolverConfigXmlAutoConfigurationTest() {
        contextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(NormalSpringTestConfiguration.class);
        emptyContextRunner = new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(EmptySpringTestConfiguration.class);
    }

    @Test
    void noSolutionOrEntityClasses() {
        emptyContextRunner
                .run(context -> assertThat(context.getStartupFailure()).isNull());
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
        contextRunner
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
                        "timefold.solver.nearby-distance-meter-class=ai.timefold.solver.spring.boot.autoconfigure.basic.domain.TestdataSpringSolution")
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig).isNotNull();
                    assertThat(solverConfig.getNearbyDistanceMeterClass()).isNotNull();
                }))
                .rootCause().message().contains("The Nearby Selection Meter class",
                        "ai.timefold.solver.spring.boot.autoconfigure.basic.domain.TestdataSpringSolution");
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
                    assertEquals(Duration.ofHours(4), solverConfig.getTerminationConfig().getSpentLimit());
                    assertEquals(Duration.ofHours(5), solverConfig.getTerminationConfig().getUnimprovedSpentLimit());
                    assertEquals(SimpleScore.of(0).toString(), solverConfig.getTerminationConfig().getBestScoreLimit());
                });
        assertThatCode(() -> contextRunner
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withSystemProperties(
                        "spring.config.location=classpath:ai/timefold/solver/spring/boot/autoconfigure/single-solver/invalid-application.yaml")
                .run(context -> context.getBean(SolverConfig.class)))
                .rootCause().message().contains("Cannot use global solver properties with named solvers", "solverConfigXml",
                        "environmentMode", "moveThreadCount",
                        "domainAccessType", "Expected all values to be maps, but values for key(s)",
                        "Maybe try changing the property name to kebab-case");
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
                    SolutionManager<TestdataSpringSolution, SimpleScore> solutionManager =
                            context.getBean(SolutionManager.class);
                    assertThat(((DefaultSolverFactory) solverFactory).getScoreDirectorFactory())
                            .isSameAs(((DefaultSolutionManager<TestdataSpringSolution, SimpleScore>) solutionManager)
                                    .getScoreDirectorFactory());
                    var solverManager = context.getBean(SolverManager.class);
                    assertThat(solverManager).isNotNull();
                    // There is only one SolverFactory instance
                    assertThat(((DefaultSolverManager<TestdataSpringSolution>) solverManager).getSolverFactory())
                            .isSameAs(solverFactory);
                });
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

}
