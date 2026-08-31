package ai.timefold.solver.core.impl.score.director;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.incremental.IncrementalScoreDirector;
import ai.timefold.solver.core.impl.score.director.incremental.IncrementalScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.stream.MultiEnvironmentBavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;
import ai.timefold.solver.core.testconstraint.DummyConstraintProvider;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ScoreDirectorFactoryFactoryTest {

    private static final SolutionDescriptor<TestdataSolution> SOLUTION_DESCRIPTOR =
            TestdataSolution.buildSolutionDescriptor();

    /**
     * A factory factory with no solver config, and therefore no phases and only ever one environment mode.
     */
    private static ScoreDirectorFactoryFactory<TestdataSolution, SimpleScore> factoryFactory(
            ScoreDirectorFactoryConfig config) {
        return new ScoreDirectorFactoryFactory<>(config);
    }

    private static ScoreDirectorFactory<TestdataSolution, SimpleScore> buildFactory(ScoreDirectorFactoryConfig config) {
        return buildFactory(config, EnvironmentMode.PHASE_ASSERT);
    }

    private static ScoreDirectorFactory<TestdataSolution, SimpleScore> buildFactory(ScoreDirectorFactoryConfig config,
            EnvironmentMode environmentMode) {
        return factoryFactory(config).buildScoreDirectorFactory(environmentMode, SOLUTION_DESCRIPTOR);
    }

    /**
     * @param phaseEnvironmentModes one entry per phase, null meaning the phase does not override the solver's mode
     */
    private static ScoreDirectorFactory<TestdataSolution, SimpleScore> buildFactoryFromSolverConfig(
            EnvironmentMode globalEnvironmentMode, EnvironmentMode... phaseEnvironmentModes) {
        return buildFactoryFromSolverConfig(constraintStreamConfig(), globalEnvironmentMode, phaseEnvironmentModes);
    }

    /**
     * @param phaseEnvironmentModes one entry per phase, null meaning the phase does not override the solver's mode
     */
    private static ScoreDirectorFactory<TestdataSolution, SimpleScore> buildFactoryFromSolverConfig(
            ScoreDirectorFactoryConfig scoreDirectorFactoryConfig, EnvironmentMode globalEnvironmentMode,
            EnvironmentMode... phaseEnvironmentModes) {
        var constructionHeuristicPhaseConfig = new ConstructionHeuristicPhaseConfig();
        constructionHeuristicPhaseConfig.setEnvironmentMode(phaseEnvironmentModes[0]);
        var localSearchPhaseConfig = new LocalSearchPhaseConfig();
        localSearchPhaseConfig.setEnvironmentMode(phaseEnvironmentModes[1]);
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEnvironmentMode(globalEnvironmentMode)
                .withScoreDirectorFactory(scoreDirectorFactoryConfig)
                .withPhases(constructionHeuristicPhaseConfig, localSearchPhaseConfig);
        return new ScoreDirectorFactoryFactory<TestdataSolution, SimpleScore>(solverConfig)
                .buildScoreDirectorFactory(globalEnvironmentMode, SOLUTION_DESCRIPTOR);
    }

    private static ScoreDirectorFactoryConfig easyConfig() {
        return new ScoreDirectorFactoryConfig()
                .withEasyScoreCalculatorClass(TestCustomPropertiesEasyScoreCalculator.class);
    }

    private static ScoreDirectorFactoryConfig incrementalConfig() {
        return new ScoreDirectorFactoryConfig()
                .withIncrementalScoreCalculatorClass(TestCustomPropertiesIncrementalScoreCalculator.class);
    }

    private static ScoreDirectorFactoryConfig constraintStreamConfig() {
        return new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(DummyConstraintProvider.class);
    }

    // ************************************************************************
    // Picking the implementation
    // ************************************************************************

    @Test
    void easyScoreCalculator() {
        assertThat(buildFactory(easyConfig()))
                .isExactlyInstanceOf(EasyScoreDirectorFactory.class);
    }

    @Test
    void incrementalScoreCalculator() {
        assertThat(buildFactory(incrementalConfig()))
                .isExactlyInstanceOf(IncrementalScoreDirectorFactory.class);
    }

    @Test
    void constraintStreams() {
        assertThat(buildFactory(constraintStreamConfig()))
                .isExactlyInstanceOf(BavetConstraintStreamScoreDirectorFactory.class);
    }

    @Test
    void factoryIsSharedWithTheScoreDirectorsItBuilds() {
        var scoreDirectorFactory = buildFactory(incrementalConfig());
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector()) {
            assertThat(scoreDirector.getScoreDirectorFactory()).isSameAs(scoreDirectorFactory);
            assertThat(scoreDirector.getSolutionDescriptor())
                    .isSameAs(scoreDirectorFactory.getSolutionDescriptor());
            assertThat(scoreDirector.getScoreDefinition())
                    .isSameAs(scoreDirectorFactory.getScoreDefinition());
        }
    }

    @Test
    void noScoreCalculation_throwsException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> buildFactory(new ScoreDirectorFactoryConfig()))
                .withMessageContaining("lacks configuration");
    }

    @Test
    void solverConfigWithoutScoreDirectorFactory_throwsException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new ScoreDirectorFactoryFactory<TestdataSolution, SimpleScore>(new SolverConfig())
                        .buildScoreDirectorFactory(EnvironmentMode.PHASE_ASSERT, SOLUTION_DESCRIPTOR))
                .withMessageContaining("lacks configuration");
    }

    @Test
    void multipleScoreCalculations_throwsException() {
        var config = constraintStreamConfig()
                .withEasyScoreCalculatorClass(TestCustomPropertiesEasyScoreCalculator.class);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> factoryFactory(config))
                .withMessageContaining("scoreDirectorFactory")
                .withMessageContaining("together");
    }

    @Test
    void incrementalMultipleScoreCalculations_throwsException() {
        var config = constraintStreamConfig()
                .withIncrementalScoreCalculatorClass(TestCustomPropertiesIncrementalScoreCalculator.class);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> factoryFactory(config))
                .withMessageContaining("scoreDirectorFactory")
                .withMessageContaining("together");
    }

    // ************************************************************************
    // Deciding whether more than one environment mode is served
    // ************************************************************************

    @Test
    void noPhaseOverrideYieldsTheConcreteFactory() {
        // Nothing runs in another mode, so there is nothing to adapt.
        assertThat(buildFactoryFromSolverConfig(EnvironmentMode.PHASE_ASSERT, null, null))
                .isExactlyInstanceOf(BavetConstraintStreamScoreDirectorFactory.class);
    }

    @Test
    void oneOverridingPhaseYieldsTheMultiEnvironmentFactory() {
        // The documented case: the solver stays at its default and only local search is asserted.
        assertThat(buildFactoryFromSolverConfig(EnvironmentMode.PHASE_ASSERT, null, EnvironmentMode.FULL_ASSERT))
                .isExactlyInstanceOf(MultiEnvironmentBavetConstraintStreamScoreDirectorFactory.class);
    }

    @Test
    void everyPhaseOverridingYieldsTheMultiEnvironmentFactory() {
        // No phase runs in the solver's mode, but the solver's mode still has to be served to everything
        // outside the phases, so two factories are needed all the same.
        assertThat(buildFactoryFromSolverConfig(EnvironmentMode.PHASE_ASSERT,
                EnvironmentMode.FULL_ASSERT, EnvironmentMode.FULL_ASSERT))
                .isExactlyInstanceOf(MultiEnvironmentBavetConstraintStreamScoreDirectorFactory.class);
    }

    @Test
    void phasesRestatingTheGlobalModeYieldTheConcreteFactory() {
        // Restating the solver's own mode is not running in another mode.
        assertThat(buildFactoryFromSolverConfig(EnvironmentMode.PHASE_ASSERT,
                EnvironmentMode.PHASE_ASSERT, EnvironmentMode.PHASE_ASSERT))
                .isExactlyInstanceOf(BavetConstraintStreamScoreDirectorFactory.class);
    }

    @Test
    void multiEnvironmentFactoryCarriesTheConfiguredTrend() {
        // The trend is applied to the concrete factory before it is adapted, so the adapted factory has to
        // carry it over; the solver reads it from there to build its HeuristicConfigPolicy.
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withScoreDirectorFactory(constraintStreamConfig().withInitializingScoreTrend("ONLY_DOWN"))
                .withPhases(new LocalSearchPhaseConfig().withEnvironmentMode(EnvironmentMode.FULL_ASSERT));
        var scoreDirectorFactory = new ScoreDirectorFactoryFactory<TestdataSolution, SimpleScore>(solverConfig)
                .buildScoreDirectorFactory(EnvironmentMode.PHASE_ASSERT, SOLUTION_DESCRIPTOR);
        assertThat(scoreDirectorFactory).isExactlyInstanceOf(MultiEnvironmentBavetConstraintStreamScoreDirectorFactory.class);
        assertThat(scoreDirectorFactory.getInitializingScoreTrend())
                .isEqualTo(InitializingScoreTrend.parseTrend("ONLY_DOWN", 1));
    }

    @Test
    void multiEnvironmentFactoryCarriesTheConfiguredAssertionFactory() {
        // The other value applied before adapting. Losing it is silent: score corruption would then be
        // asserted against the same implementation instead of the independent one configured here.
        // STEP_ASSERT because an assertionScoreDirectorFactory requires that mode or stricter.
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.STEP_ASSERT)
                .withScoreDirectorFactory(constraintStreamConfig()
                        .withAssertionScoreDirectorFactory(incrementalConfig()))
                .withPhases(new LocalSearchPhaseConfig().withEnvironmentMode(EnvironmentMode.FULL_ASSERT));
        var scoreDirectorFactory = new ScoreDirectorFactoryFactory<TestdataSolution, SimpleScore>(solverConfig)
                .buildScoreDirectorFactory(EnvironmentMode.STEP_ASSERT, SOLUTION_DESCRIPTOR);
        assertThat(scoreDirectorFactory).isExactlyInstanceOf(MultiEnvironmentBavetConstraintStreamScoreDirectorFactory.class);

        var assertionScoreDirectorFactory =
                ((AbstractScoreDirectorFactory<TestdataSolution, ?, ?>) scoreDirectorFactory)
                        .getAssertionScoreDirectorFactory();
        // Carried over from the factory it took over from, and a concrete one, as the code using it expects.
        assertThat(assertionScoreDirectorFactory)
                .isExactlyInstanceOf(IncrementalScoreDirectorFactory.class);
    }

    @Test
    void easyScoreCalculatorServesEveryEnvironmentModeItself() {
        // An easy calculator has no state built from the environment mode; the mode is passed straight on to
        // the score director, so one factory serves every mode and there is nothing to adapt.
        assertThat(buildFactoryFromSolverConfig(easyConfig(), EnvironmentMode.PHASE_ASSERT, null,
                EnvironmentMode.FULL_ASSERT))
                .isExactlyInstanceOf(EasyScoreDirectorFactory.class);
    }

    @Test
    void incrementalScoreCalculatorServesEveryEnvironmentModeItself() {
        assertThat(buildFactoryFromSolverConfig(incrementalConfig(), EnvironmentMode.PHASE_ASSERT, null,
                EnvironmentMode.FULL_ASSERT))
                .isExactlyInstanceOf(IncrementalScoreDirectorFactory.class);
    }

    @Test
    void aFactoryWhichServesEveryModeItselfStillHonoursTheRequestedMode() {
        // Which is what makes adapting to itself correct rather than merely tolerated.
        var scoreDirectorFactory = buildFactoryFromSolverConfig(easyConfig(), EnvironmentMode.PHASE_ASSERT, null,
                EnvironmentMode.FULL_ASSERT);
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector(EnvironmentMode.FULL_ASSERT)) {
            assertThat(scoreDirector.environmentMode).isEqualTo(EnvironmentMode.FULL_ASSERT);
            assertThat(scoreDirector.getScoreDirectorFactory()).isSameAs(scoreDirectorFactory);
        }
    }

    // ************************************************************************
    // Serving more than one environment mode
    // ************************************************************************

    @Test
    void globalEnvironmentModeIsServedByTheFactoryItself() {
        var scoreDirectorFactory = (MultiEnvironmentBavetConstraintStreamScoreDirectorFactory<TestdataSolution, SimpleScore>) //
        buildFactoryFromSolverConfig(EnvironmentMode.PHASE_ASSERT, null, EnvironmentMode.FULL_ASSERT);
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector(EnvironmentMode.PHASE_ASSERT)) {
            assertThat(scoreDirector.environmentMode).isEqualTo(EnvironmentMode.PHASE_ASSERT);
            // It took over the constraint network built for the solver's own mode, so no second factory is
            // needed for that mode, and none is built.
            assertThat(scoreDirector.getScoreDirectorFactory()).isSameAs(scoreDirectorFactory);
        }
    }

    @Test
    void otherEnvironmentModeGetsAFactoryOfItsOwn() {
        var scoreDirectorFactory = (MultiEnvironmentBavetConstraintStreamScoreDirectorFactory<TestdataSolution, SimpleScore>) //
        buildFactoryFromSolverConfig(EnvironmentMode.PHASE_ASSERT, null, EnvironmentMode.FULL_ASSERT);
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector(EnvironmentMode.FULL_ASSERT)) {
            assertThat(scoreDirector.environmentMode).isEqualTo(EnvironmentMode.FULL_ASSERT);
            // Constraint Streams builds its network from the mode, so serving this one from the inherited
            // network would have given a score director whose network does not match the mode it reports.
            assertThat(scoreDirector.getScoreDirectorFactory())
                    .isNotSameAs(scoreDirectorFactory)
                    .isExactlyInstanceOf(BavetConstraintStreamScoreDirectorFactory.class);
        }
    }

    @Test
    void theFactoryForAnotherEnvironmentModeIsBuiltOnceAndShared() {
        var scoreDirectorFactory = (MultiEnvironmentBavetConstraintStreamScoreDirectorFactory<TestdataSolution, SimpleScore>) //
        buildFactoryFromSolverConfig(EnvironmentMode.PHASE_ASSERT, null, EnvironmentMode.FULL_ASSERT);
        try (var first = scoreDirectorFactory.buildScoreDirector(EnvironmentMode.FULL_ASSERT);
                var second = scoreDirectorFactory.buildScoreDirector(EnvironmentMode.FULL_ASSERT)) {
            // Building a constraint network is expensive; building a score director on top of one is not.
            assertThat(second.getScoreDirectorFactory()).isSameAs(first.getScoreDirectorFactory());
            assertThat(first).isNotSameAs(second);
        }
    }

    @ParameterizedTest
    @EnumSource(ChildThreadType.class)
    void childThreadScoreDirectorKeepsTheRequestedEnvironmentMode(ChildThreadType childThreadType) {
        var scoreDirectorFactory = buildFactoryFromSolverConfig(EnvironmentMode.PHASE_ASSERT, null,
                EnvironmentMode.FULL_ASSERT);
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector(EnvironmentMode.FULL_ASSERT)) {
            scoreDirector.setWorkingSolution(TestdataSolution.generateSolution());
            // The child thread runs the phase's assertions, not the solver's laxer default.
            try (var childScoreDirector = (AbstractScoreDirector<TestdataSolution, SimpleScore, ?>) scoreDirector
                    .createChildThreadScoreDirector(childThreadType)) {
                assertThat(childScoreDirector.environmentMode).isEqualTo(EnvironmentMode.FULL_ASSERT);
            }
        }
    }

    // ************************************************************************
    // Constraint match policy
    // ************************************************************************

    @Test
    void constraintMatchDisabledUnlessRequested() {
        var scoreDirectorFactory = buildFactory(constraintStreamConfig(), EnvironmentMode.FULL_ASSERT);
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector()) {
            // The environment mode alone does not enable constraint matching;
            // the caller has to apply decideConstraintMatchPolicy() to the builder.
            assertThat(scoreDirector.getConstraintMatchPolicy()).isEqualTo(ConstraintMatchPolicy.DISABLED);
        }
    }

    @Test
    void constraintMatchEnabledByEnvironmentMode() {
        var solverScope = solverScopeWith();
        assertThat(ScoreDirectorFactoryFactory.decideConstraintMatchPolicy(solverScope, EnvironmentMode.PHASE_ASSERT))
                .isEqualTo(ConstraintMatchPolicy.DISABLED);
        assertThat(ScoreDirectorFactoryFactory.decideConstraintMatchPolicy(solverScope, EnvironmentMode.FULL_ASSERT))
                .isEqualTo(ConstraintMatchPolicy.ENABLED);
    }

    @Test
    void constraintMatchAppliedToTheBuilderReachesTheScoreDirector() {
        var scoreDirectorFactory = buildFactory(constraintStreamConfig());
        var phaseEnvironmentMode = EnvironmentMode.FULL_ASSERT;
        try (var scoreDirector = scoreDirectorFactory.createScoreDirectorBuilder(phaseEnvironmentMode)
                .withConstraintMatchPolicy(
                        ScoreDirectorFactoryFactory.decideConstraintMatchPolicy(solverScopeWith(), phaseEnvironmentMode))
                .build()) {
            assertThat(scoreDirector.getConstraintMatchPolicy()).isEqualTo(ConstraintMatchPolicy.ENABLED);
        }
    }

    @Test
    void constraintMatchEnabledByMetric() {
        assertThat(ScoreDirectorFactoryFactory.decideConstraintMatchPolicy(
                solverScopeWith(SolverMetric.CONSTRAINT_MATCH_TOTAL_BEST_SCORE), EnvironmentMode.NO_ASSERT))
                .isEqualTo(ConstraintMatchPolicy.ENABLED);
    }

    @Test
    void constraintMatchNotEnabledByOtherMetric() {
        assertThat(ScoreDirectorFactoryFactory.decideConstraintMatchPolicy(
                solverScopeWith(SolverMetric.BEST_SCORE), EnvironmentMode.NO_ASSERT))
                .isEqualTo(ConstraintMatchPolicy.DISABLED);
    }

    private static SolverScope<TestdataSolution> solverScopeWith(SolverMetric... solverMetrics) {
        var solverScope = new SolverScope<TestdataSolution>();
        solverScope.setSolverMetricSet(solverMetrics.length == 0
                ? EnumSet.noneOf(SolverMetric.class)
                : EnumSet.copyOf(List.of(solverMetrics)));
        return solverScope;
    }

    // ************************************************************************
    // Configuration shared by all implementations
    // ************************************************************************

    @Test
    void incrementalScoreCalculatorWithCustomProperties() {
        var config = incrementalConfig();
        var customProperties = new HashMap<String, String>();
        customProperties.put("stringProperty", "string 1");
        customProperties.put("intProperty", "7");
        config.setIncrementalScoreCalculatorCustomProperties(customProperties);

        var scoreDirectorFactory = buildFactory(config);
        try (var scoreDirector = (IncrementalScoreDirector<TestdataSolution, SimpleScore>) scoreDirectorFactory
                .buildScoreDirector()) {
            var scoreCalculator =
                    (TestCustomPropertiesIncrementalScoreCalculator) scoreDirector.getIncrementalScoreCalculator();
            assertThat(scoreCalculator.getStringProperty()).isEqualTo("string 1");
            assertThat(scoreCalculator.getIntProperty()).isEqualTo(7);
        }
    }

    @Test
    void initializingScoreTrendFromConfig() {
        var scoreDirectorFactory = buildFactory(incrementalConfig().withInitializingScoreTrend("ONLY_DOWN"));
        assertThat(scoreDirectorFactory.getInitializingScoreTrend())
                .isEqualTo(InitializingScoreTrend.parseTrend("ONLY_DOWN", 1));
    }

    @Test
    void initializingScoreTrendDefaultsToAny() {
        assertThat(buildFactory(incrementalConfig()).getInitializingScoreTrend())
                .isEqualTo(InitializingScoreTrend.parseTrend("ANY", 1));
    }

    @Test
    void buildWithAssertionScoreDirectorFactory() {
        var config = incrementalConfig()
                .withAssertionScoreDirectorFactory(incrementalConfig());
        var scoreDirectorFactory = buildFactory(config, EnvironmentMode.STEP_ASSERT);

        var assertionScoreDirectorFactory =
                ((AbstractScoreDirectorFactory<TestdataSolution, ?, ?>) scoreDirectorFactory)
                        .getAssertionScoreDirectorFactory();
        // A concrete factory, as the code reading it expects one it can use directly.
        assertThat(assertionScoreDirectorFactory).isExactlyInstanceOf(IncrementalScoreDirectorFactory.class);
        var incrementalAssertionFactory =
                (IncrementalScoreDirectorFactory<TestdataSolution, SimpleScore>) assertionScoreDirectorFactory;
        try (var assertionScoreDirector =
                (IncrementalScoreDirector<TestdataSolution, SimpleScore>) incrementalAssertionFactory.buildScoreDirector()) {
            // The assertion score director always runs in NON_REPRODUCIBLE, regardless of the requested mode.
            assertThat(assertionScoreDirector.environmentMode).isEqualTo(EnvironmentMode.NON_REPRODUCIBLE);
            assertThat(assertionScoreDirector.getIncrementalScoreCalculator())
                    .isExactlyInstanceOf(TestCustomPropertiesIncrementalScoreCalculator.class);
        }
    }

    @Test
    void nestedAssertionScoreDirectorFactory_throwsException() {
        var config = incrementalConfig()
                .withAssertionScoreDirectorFactory(incrementalConfig()
                        .withAssertionScoreDirectorFactory(incrementalConfig()));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> buildFactory(config, EnvironmentMode.STEP_ASSERT))
                .withMessageContaining("cannot have a non-null assertionScoreDirectorFactory");
    }

    @Test
    void assertionScoreDirectorFactoryInLenientEnvironmentMode_throwsException() {
        var config = incrementalConfig()
                .withAssertionScoreDirectorFactory(incrementalConfig());
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> buildFactory(config, EnvironmentMode.PHASE_ASSERT))
                .withMessageContaining("requires an environmentMode")
                .withMessageContaining(EnvironmentMode.STEP_ASSERT.name());
    }

    public static class TestCustomPropertiesEasyScoreCalculator
            implements EasyScoreCalculator<TestdataSolution, SimpleScore> {

        private String stringProperty;
        private int intProperty;

        public String getStringProperty() {
            return stringProperty;
        }

        @SuppressWarnings("unused")
        public void setStringProperty(String stringProperty) {
            this.stringProperty = stringProperty;
        }

        public int getIntProperty() {
            return intProperty;
        }

        @SuppressWarnings("unused")
        public void setIntProperty(int intProperty) {
            this.intProperty = intProperty;
        }

        @Override
        public @NonNull SimpleScore calculateScore(@NonNull TestdataSolution testdataSolution) {
            return SimpleScore.ZERO;
        }
    }

    @NullMarked
    public static class TestCustomPropertiesIncrementalScoreCalculator
            implements IncrementalScoreCalculator<TestdataSolution, SimpleScore> {

        private String stringProperty;
        private int intProperty;

        public String getStringProperty() {
            return stringProperty;
        }

        public void setStringProperty(String stringProperty) {
            this.stringProperty = stringProperty;
        }

        public int getIntProperty() {
            return intProperty;
        }

        public void setIntProperty(int intProperty) {
            this.intProperty = intProperty;
        }

        @Override
        public void resetWorkingSolution(TestdataSolution workingSolution) {
            // No actions
        }

        @Override
        public void beforeVariableChanged(Object entity, String variableName) {
            // No actions
        }

        @Override
        public void afterVariableChanged(Object entity, String variableName) {
            // No actions
        }

        @Override
        public SimpleScore calculateScore() {
            return SimpleScore.ZERO;
        }
    }

}
