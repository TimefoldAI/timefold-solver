package ai.timefold.solver.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.spring.boot.autoconfigure.neighborhood.MultipleNeighborhoodSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.neighborhood.NeighborhoodSpringTestConfiguration;
import ai.timefold.solver.spring.boot.autoconfigure.neighborhood.multiple.TestdataOtherSpringMoveSelectionStrategy;
import ai.timefold.solver.spring.boot.autoconfigure.neighborhood.multiple.TestdataOtherSpringNeighborhoodProvider;
import ai.timefold.solver.spring.boot.autoconfigure.neighborhood.single.TestdataSpringMoveSelectionStrategy;
import ai.timefold.solver.spring.boot.autoconfigure.neighborhood.single.TestdataSpringNeighborhoodProvider;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TimefoldSolverNeighborhoodAutoConfigurationTest {

    private static final String PHASE_LEVEL_XML =
            "ai/timefold/solver/spring/boot/autoconfigure/neighborhoodPhaseLevelSolverConfig.xml";

    private final ApplicationContextRunner singleRunner = runner(NeighborhoodSpringTestConfiguration.class);
    private final ApplicationContextRunner multipleRunner = runner(MultipleNeighborhoodSpringTestConfiguration.class);

    private static ApplicationContextRunner runner(Class<?> userConfiguration) {
        return new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(TimefoldSolverAutoConfiguration.class, TimefoldSolverBeanFactory.class))
                .withUserConfiguration(userConfiguration);
    }

    @Test
    void ignoredWithoutNeighborhoods() {
        singleRunner.run(context -> {
            var solverConfig = context.getBean(SolverConfig.class);
            assertThat(solverConfig.getNeighborhoodsConfig()).isNull();
        });
    }

    @Test
    void detectedAndUsed() {
        singleRunner
                .withPropertyValues("timefold.solver.enabled-preview-features=NEIGHBORHOODS")
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig.getNeighborhoodsConfig().getNeighborhoodProviderClass())
                            .isEqualTo(TestdataSpringNeighborhoodProvider.class);
                    assertThat(solverConfig.getNeighborhoodsConfig().getMoveSelectionStrategyClass())
                            .isEqualTo(TestdataSpringMoveSelectionStrategy.class);
                    assertThat(SolverFactory.create(solverConfig).buildSolver()).isNotNull();
                });
    }

    @Test
    void multipleClassesWithOneSolverFail() {
        assertThatCode(() -> multipleRunner
                .withPropertyValues("timefold.solver.enabled-preview-features=NEIGHBORHOODS")
                .run(context -> context.getBean(SolverConfig.class)))
                .cause().message().contains("Multiple classes",
                        TestdataSpringNeighborhoodProvider.class.getSimpleName(),
                        TestdataOtherSpringNeighborhoodProvider.class.getSimpleName(),
                        "NeighborhoodProvider");
    }

    @Test
    void phaseLevelOnEveryPhaseNeedsNoDetection() {
        multipleRunner
                .withPropertyValues("timefold.solver-config-xml=" + PHASE_LEVEL_XML)
                .run(context -> {
                    var solverConfig = context.getBean(SolverConfig.class);
                    assertThat(solverConfig.getNeighborhoodsConfig()).isNull();
                    var phaseList = solverConfig.getPhaseConfigList().stream()
                            .map(LocalSearchPhaseConfig.class::cast)
                            .toList();
                    assertThat(phaseList)
                            .<Class<?>> extracting(
                                    phaseConfig -> phaseConfig.getNeighborhoodsConfig().getNeighborhoodProviderClass())
                            .containsExactly(TestdataSpringNeighborhoodProvider.class,
                                    TestdataOtherSpringNeighborhoodProvider.class);
                    assertThat(phaseList)
                            .<Class<?>> extracting(
                                    phaseConfig -> phaseConfig.getNeighborhoodsConfig().getMoveSelectionStrategyClass())
                            .containsExactly(TestdataSpringMoveSelectionStrategy.class,
                                    TestdataOtherSpringMoveSelectionStrategy.class);
                });
    }

    @Test
    void namedSolversAmbiguityFails() {
        assertThatCode(() -> multipleRunner
                .withPropertyValues("timefold.solver.solver1.enabled-preview-features=NEIGHBORHOODS")
                .withPropertyValues("timefold.solver.solver2.enabled-preview-features=NEIGHBORHOODS")
                .run(context -> context.getBean("solver1")))
                .cause().message().contains("Some solver configs (solver1, solver2) don't specify a NeighborhoodProvider",
                        "multiple available");
    }

    @Test
    void unusedClassesAllowed() {
        var xml = "ai/timefold/solver/spring/boot/autoconfigure/neighborhoodSinglePhaseLevelSolverConfig.xml";
        multipleRunner
                .withPropertyValues("timefold.solver.solver1.solver-config-xml=" + xml)
                .withPropertyValues("timefold.solver.solver2.solver-config-xml=" + xml)
                .run(context -> assertThat(context.getBean("solver1")).isNotNull());
    }

    @Test
    void namedSolversWithoutNeighborhoodsIgnored() {
        multipleRunner
                .withPropertyValues("timefold.solver.solver1.termination.best-score-limit=0")
                .withPropertyValues("timefold.solver.solver2.termination.best-score-limit=0")
                .run(context -> assertThat(context.getBean("solver1")).isNotNull());
    }

}
