package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.solver.SolverConfigOverride;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.DefaultSolverTest.DummyEasyScoreCalculator;
import ai.timefold.solver.core.impl.solver.random.DelegatingSplittableRandomGenerator;
import ai.timefold.solver.core.impl.solver.random.RandomSource;
import ai.timefold.solver.core.testdomain.TestdataConstraintProvider;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.invalid.noentity.TestdataNoEntitySolution;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class DefaultSolverFactoryTest {

    @Test
    void moveThreadCountAutoIsCorrectlyResolvedWhenCpuCountIsPositive() {
        assertThat(mockMoveThreadCountResolverAuto(1)).isNull();
        assertThat(mockMoveThreadCountResolverAuto(2)).isNull();
        assertThat(mockMoveThreadCountResolverAuto(4)).isEqualTo(2);
        assertThat(mockMoveThreadCountResolverAuto(5)).isEqualTo(3);
        assertThat(mockMoveThreadCountResolverAuto(6)).isEqualTo(4);
        assertThat(mockMoveThreadCountResolverAuto(100)).isEqualTo(4);
    }

    @Test
    void moveThreadCountAutoIsResolvedToNullWhenCpuCountIsNegative() {
        assertThat(mockMoveThreadCountResolverAuto(-1)).isNull();
    }

    private Integer mockMoveThreadCountResolverAuto(int mockCpuCount) {
        DefaultSolverFactory.MoveThreadCountResolver moveThreadCountResolverMock =
                new DefaultSolverFactory.MoveThreadCountResolver() {
                    @Override
                    protected int getAvailableProcessors() {
                        return mockCpuCount;
                    }
                };
        var maybeCount = moveThreadCountResolverMock.resolveMoveThreadCount(SolverConfig.MOVE_THREAD_COUNT_AUTO);
        if (maybeCount.isPresent()) {
            return maybeCount.getAsInt();
        } else {
            return null;
        }
    }

    @Test
    void moveThreadCountIsCorrectlyResolvedWhenValueIsPositive() {
        assertThat(resolveMoveThreadCount("2")).isEqualTo(2);
    }

    @Test
    void moveThreadCountThrowsExceptionWhenValueIsNegative() {
        assertThatIllegalArgumentException().isThrownBy(() -> resolveMoveThreadCount("-1"));
    }

    @Test
    void moveThreadCountIsResolvedToNullWhenValueIsNone() {
        assertThat(resolveMoveThreadCount(SolverConfig.MOVE_THREAD_COUNT_NONE)).isNull();
    }

    private Integer resolveMoveThreadCount(String moveThreadCountString) {
        DefaultSolverFactory.MoveThreadCountResolver moveThreadCountResolver =
                new DefaultSolverFactory.MoveThreadCountResolver();
        var maybeCount = moveThreadCountResolver.resolveMoveThreadCount(moveThreadCountString);
        if (maybeCount.isPresent()) {
            return maybeCount.getAsInt();
        } else {
            return null;
        }
    }

    @Test
    void cachesScoreDirectorFactory() {
        SolverConfig solverConfig =
                SolverConfig.createFromXmlResource("ai/timefold/solver/core/config/solver/testdataSolverConfig.xml");
        DefaultSolverFactory<TestdataSolution> defaultSolverFactory = new DefaultSolverFactory<>(solverConfig);

        SolutionDescriptor<TestdataSolution> solutionDescriptor1 = defaultSolverFactory.getSolutionDescriptor();
        ScoreDirectorFactory<TestdataSolution, SimpleScore> scoreDirectorFactory1 =
                defaultSolverFactory.getScoreDirectorFactory();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor1).isNotNull();
            softly.assertThat(scoreDirectorFactory1).isNotNull();
            softly.assertThat(scoreDirectorFactory1.getSolutionDescriptor()).isSameAs(solutionDescriptor1);
        });

        SolutionDescriptor<TestdataSolution> solutionDescriptor2 = defaultSolverFactory.getSolutionDescriptor();
        ScoreDirectorFactory<TestdataSolution, SimpleScore> scoreDirectorFactory2 =
                defaultSolverFactory.getScoreDirectorFactory();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor2).isSameAs(solutionDescriptor1);
            softly.assertThat(scoreDirectorFactory2).isSameAs(scoreDirectorFactory1);
        });
    }

    @Test
    void useCorrectRandomSeed() {
        // Reproducible
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(DummyEasyScoreCalculator.class)
                .withRandomSeed(123456L);
        var defaultSolverFactory = new DefaultSolverFactory<TestdataSolution>(solverConfig);
        var randomGenerator = (DelegatingSplittableRandomGenerator) defaultSolverFactory
                .buildRandomSupplier(EnvironmentMode.PHASE_ASSERT).get().moveIteratorUsage();
        var otherRandomGenerator =
                (DelegatingSplittableRandomGenerator) RandomSource.seeded(solverConfig.getRandomSeed()).moveIteratorUsage();
        assertThat(randomGenerator.getSeed()).isEqualTo(otherRandomGenerator.getSeed());
        assertThat(otherRandomGenerator.nextLong()).isEqualTo(randomGenerator.nextLong());
    }

    @Test
    void testNoSolutionConfiguration() {
        SolverConfig solverConfig = new SolverConfig();
        assertThatCode(() -> new DefaultSolverFactory<>(solverConfig))
                .hasMessageContaining("The solver configuration must have a solutionClass")
                .hasMessageContaining(
                        "If you're using the Quarkus extension or Spring Boot starter, it should have been filled in already.");
    }

    @Test
    void testNoEntityConfiguration() {
        SolverConfig solverConfig = new SolverConfig();
        solverConfig.setSolutionClass(TestdataNoEntitySolution.class);
        assertThatCode(() -> new DefaultSolverFactory<>(solverConfig))
                .hasMessageContaining("The solver configuration must have at least 1 entityClass")
                .hasMessageContaining(
                        "If you're using the Quarkus extension or Spring Boot starter, it should have been filled in already.");
    }

    @Test
    void testInvalidMoveThreadCountConfiguration() {
        SolverConfig solverConfig =
                SolverConfig.createFromXmlResource("ai/timefold/solver/core/config/solver/testdataSolverConfig.xml")
                        .withMoveThreadCount("-1");
        assertThatCode(() -> new DefaultSolverFactory<>(solverConfig).buildSolver(new SolverConfigOverride()))
                .hasMessageContaining("The moveThreadCount")
                .hasMessageContaining("resulted in a resolvedMoveThreadCount")
                .hasMessageContaining("that is lower than 1.");
    }

    @Test
    void testInvalidConstraintProfilingWithoutEnterprise() {
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withConstraintProviderClass(TestdataConstraintProvider.class)
                        .withConstraintStreamProfilingEnabled(true));
        assertThatCode(() -> new DefaultSolverFactory<>(solverConfig).buildSolver(new SolverConfigOverride()))
                .hasMessageContainingAll("Constraint profiling",
                        "remove constraintStreamProfilingEnabled from the solver configuration");
    }

    @Test
    void assertEnvironmentModeWithoutPhases() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(DummyEasyScoreCalculator.class)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        assertThatCode(() -> new DefaultSolverFactory<>(solverConfig)).doesNotThrowAnyException();
    }

    @Test
    void assertEnvironmentModeWithValidPhases() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        // The default environment mode must be used by at least one phase,
        // and every phase must be at least as strict as the default.
        solverConfig.getPhaseConfigList().getFirst().setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT);
        assertThatCode(() -> new DefaultSolverFactory<>(solverConfig)).doesNotThrowAnyException();
    }

    @Test
    void assertEnvironmentWithNonReproducibleAndMismatchingPhase() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.NON_REPRODUCIBLE);
        solverConfig.getPhaseConfigList().get(0).setEnvironmentMode(EnvironmentMode.NON_REPRODUCIBLE);
        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.NO_ASSERT);
        assertThatCode(() -> new DefaultSolverFactory<>(solverConfig))
                .hasMessageContaining("is only possible when global environmentMode is reproducible");
    }

    @Test
    void assertEnvironmentModeWithGlobalNotUsedByAnyPhase() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.STEP_ASSERT);
        solverConfig.getPhaseConfigList().get(0).setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.NON_INTRUSIVE_FULL_ASSERT);
        // Every phase may override the global mode; it still governs everything outside the phases.
        assertThatCode(() -> new DefaultSolverFactory<>(solverConfig)).doesNotThrowAnyException();
    }

    @Test
    void identicalPhaseEnvironmentModesBecomeTheGlobalEnvironmentMode() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        solverConfig.getPhaseConfigList()
                .forEach(phaseConfig -> phaseConfig.setEnvironmentMode(EnvironmentMode.FULL_ASSERT));
        var solver = (AbstractSolver<TestdataSolution>) SolverFactory.<TestdataSolution> create(solverConfig)
                .buildSolver();
        // Every phase agrees, so there is nothing for the global mode to differ from:
        // adopting it spares the solver a second score director factory for a mode no phase ever runs in.
        assertThat(solver.globalEnvironmentMode).isEqualTo(EnvironmentMode.FULL_ASSERT);
    }

    @Test
    void differingPhaseEnvironmentModesLeaveTheGlobalEnvironmentModeAlone() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.STEP_ASSERT);
        solverConfig.getPhaseConfigList().get(0).setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.NON_INTRUSIVE_FULL_ASSERT);
        var solver = (AbstractSolver<TestdataSolution>) SolverFactory.<TestdataSolution> create(solverConfig)
                .buildSolver();
        assertThat(solver.globalEnvironmentMode).isEqualTo(EnvironmentMode.STEP_ASSERT);
    }

    @Test
    void onePhaseLeftOnTheGlobalEnvironmentMode() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        // Only the second phase overrides; the first still runs in the global mode, so it must stay.
        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        var solver = (AbstractSolver<TestdataSolution>) SolverFactory.<TestdataSolution> create(solverConfig)
                .buildSolver();
        assertThat(solver.globalEnvironmentMode).isEqualTo(EnvironmentMode.PHASE_ASSERT);
    }

    @Test
    void phaseEnvironmentModeCannotMakeTheGlobalNonReproducible() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        solverConfig.getPhaseConfigList()
                .forEach(phaseConfig -> phaseConfig.setEnvironmentMode(EnvironmentMode.NON_REPRODUCIBLE));
        // NON_REPRODUCIBLE is the most lenient mode, so it is rejected as an override before adoption is
        // ever considered. Otherwise a phase-level setting could silently cost the solver its reproducibility.
        assertThatCode(() -> new DefaultSolverFactory<>(solverConfig))
                .hasMessageContaining("must have an assertion level higher than or equal to the global environment level");
    }

    @Test
    void solvesWithEveryPhaseOverridingAnUnsetGlobalEnvironmentMode() {
        // "Assert everything", expressed per phase, with the solver-level mode left at its default.
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        solverConfig.getPhaseConfigList().forEach(phaseConfig -> phaseConfig
                .setEnvironmentMode(EnvironmentMode.FULL_ASSERT));
        var solution = SolverFactory.<TestdataSolution> create(solverConfig)
                .buildSolver()
                .solve(TestdataSolution.generateSolution(2, 2));
        assertThat(solution).isNotNull();
        assertThat(solution.getScore()).isNotNull();
    }

    @Test
    void assertEnvironmentModeWithPhaseLessStrictThanDefault() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.STEP_ASSERT);
        solverConfig.getPhaseConfigList().get(0).setEnvironmentMode(EnvironmentMode.STEP_ASSERT);
        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.NO_ASSERT);
        assertThatCode(() -> new DefaultSolverFactory<>(solverConfig))
                .hasMessageContaining(
                        "must have an assertion level higher than or equal to the global environment level");
    }

}
