package ai.timefold.solver.core.impl.score.director;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.HashMap;
import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.monitoring.MonitoringConfig;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.incremental.IncrementalScoreDirector;
import ai.timefold.solver.core.impl.score.director.incremental.IncrementalScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;
import ai.timefold.solver.core.testconstraint.DummyConstraintProvider;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

class DelegateScoreDirectorFactoryTest {

    private static DelegateScoreDirectorFactory<TestdataSolution, SimpleScore> buildTestdataScoreDirectorFactory(
            ScoreDirectorFactoryConfig config, EnvironmentMode environmentMode) {
        return new DelegateScoreDirectorFactory<>(config, TestdataSolution.buildSolutionDescriptor(), environmentMode);
    }

    private static DelegateScoreDirectorFactory<TestdataSolution, SimpleScore> buildTestdataScoreDirectorFactory(
            ScoreDirectorFactoryConfig config) {
        return buildTestdataScoreDirectorFactory(config, EnvironmentMode.PHASE_ASSERT);
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
    // Picking the delegate
    // ************************************************************************

    @Test
    void easyScoreCalculatorDelegate() {
        assertThat(buildTestdataScoreDirectorFactory(easyConfig()).getDelegate())
                .isExactlyInstanceOf(EasyScoreDirectorFactory.class);
    }

    @Test
    void incrementalScoreCalculatorDelegate() {
        assertThat(buildTestdataScoreDirectorFactory(incrementalConfig()).getDelegate())
                .isExactlyInstanceOf(IncrementalScoreDirectorFactory.class);
    }

    @Test
    void constraintStreamsDelegate() {
        assertThat(buildTestdataScoreDirectorFactory(constraintStreamConfig()).getDelegate())
                .isExactlyInstanceOf(BavetConstraintStreamScoreDirectorFactory.class);
    }

    @Test
    void delegateIsSharedWithTheScoreDirectorsItBuilds() {
        var scoreDirectorFactory = buildTestdataScoreDirectorFactory(incrementalConfig());
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector()) {
            assertThat(scoreDirector.getScoreDirectorFactory()).isSameAs(scoreDirectorFactory.getDelegate());
        }
        assertThat(scoreDirectorFactory.getSolutionDescriptor())
                .isSameAs(scoreDirectorFactory.getDelegate().getSolutionDescriptor());
        assertThat(scoreDirectorFactory.getScoreDefinition())
                .isSameAs(scoreDirectorFactory.getDelegate().getScoreDefinition());
    }

    @Test
    void noScoreCalculation_throwsException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> buildTestdataScoreDirectorFactory(new ScoreDirectorFactoryConfig()))
                .withMessageContaining("lacks configuration");
    }

    @Test
    void solverConfigWithoutScoreDirectorFactory_throwsException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new DelegateScoreDirectorFactory<TestdataSolution, SimpleScore>(new SolverConfig(),
                        TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT))
                .withMessageContaining("lacks configuration");
    }

    @Test
    void multipleScoreCalculations_throwsException() {
        var config = constraintStreamConfig()
                .withEasyScoreCalculatorClass(TestCustomPropertiesEasyScoreCalculator.class);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> buildTestdataScoreDirectorFactory(config))
                .withMessageContaining("scoreDirectorFactory")
                .withMessageContaining("together");
    }

    @Test
    void incrementalMultipleScoreCalculations_throwsException() {
        var config = constraintStreamConfig()
                .withIncrementalScoreCalculatorClass(TestCustomPropertiesIncrementalScoreCalculator.class);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> buildTestdataScoreDirectorFactory(config))
                .withMessageContaining("scoreDirectorFactory")
                .withMessageContaining("together");
    }

    // ************************************************************************
    // Environment modes
    // ************************************************************************

    @Test
    void globalEnvironmentModeIsUsedWhenNoneRequested() {
        var scoreDirectorFactory = buildTestdataScoreDirectorFactory(incrementalConfig(), EnvironmentMode.FULL_ASSERT);
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector()) {
            // The environment mode is not exposed by the score director; the test lives in the same package to read it.
            assertThat(scoreDirector.environmentMode).isEqualTo(EnvironmentMode.FULL_ASSERT);
        }
    }

    @Test
    void otherEnvironmentModeReusesDelegate() {
        var scoreDirectorFactory = buildTestdataScoreDirectorFactory(incrementalConfig(), EnvironmentMode.PHASE_ASSERT);
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector(EnvironmentMode.FULL_ASSERT)) {
            assertThat(scoreDirector.environmentMode).isEqualTo(EnvironmentMode.FULL_ASSERT);
            // Only the constraint stream factory depends on the environment mode; the others are reused as they are.
            assertThat(scoreDirector.getScoreDirectorFactory()).isSameAs(scoreDirectorFactory.getDelegate());
        }
    }

    @Test
    void otherEnvironmentModeRebuildsConstraintStreamDelegate() {
        var scoreDirectorFactory = buildTestdataScoreDirectorFactory(constraintStreamConfig(), EnvironmentMode.PHASE_ASSERT);
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector(EnvironmentMode.FULL_ASSERT)) {
            assertThat(scoreDirector.environmentMode).isEqualTo(EnvironmentMode.FULL_ASSERT);
            // The constraint network is built from the environment mode, so the cached delegate cannot be reused.
            assertThat(scoreDirector.getScoreDirectorFactory())
                    .isExactlyInstanceOf(BavetConstraintStreamScoreDirectorFactory.class)
                    .isNotSameAs(scoreDirectorFactory.getDelegate());
        }
    }

    @Test
    void globalEnvironmentModeReusesConstraintStreamDelegate() {
        var scoreDirectorFactory = buildTestdataScoreDirectorFactory(constraintStreamConfig(), EnvironmentMode.PHASE_ASSERT);
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector(EnvironmentMode.PHASE_ASSERT)) {
            assertThat(scoreDirector.getScoreDirectorFactory()).isSameAs(scoreDirectorFactory.getDelegate());
        }
    }

    // ************************************************************************
    // Constraint match policy
    // ************************************************************************

    @Test
    void constraintMatchDisabledUnlessRequested() {
        var scoreDirectorFactory = buildTestdataScoreDirectorFactory(constraintStreamConfig(), EnvironmentMode.FULL_ASSERT);
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector()) {
            // The environment mode alone does not enable constraint matching;
            // the caller has to apply decideConstraintMatchPolicy() to the builder.
            assertThat(scoreDirector.getConstraintMatchPolicy()).isEqualTo(ConstraintMatchPolicy.DISABLED);
        }
    }

    @Test
    void constraintMatchEnabledPerPhaseEnvironmentMode() {
        var scoreDirectorFactory = buildTestdataScoreDirectorFactory(constraintStreamConfig(), EnvironmentMode.PHASE_ASSERT);
        assertThat(scoreDirectorFactory.decideConstraintMatchPolicy(EnvironmentMode.PHASE_ASSERT))
                .isEqualTo(ConstraintMatchPolicy.DISABLED);
        assertThat(scoreDirectorFactory.decideConstraintMatchPolicy(EnvironmentMode.FULL_ASSERT))
                .isEqualTo(ConstraintMatchPolicy.ENABLED);

        var phaseEnvironmentMode = EnvironmentMode.FULL_ASSERT;
        try (var scoreDirector = scoreDirectorFactory.createScoreDirectorBuilder(phaseEnvironmentMode)
                .withConstraintMatchPolicy(scoreDirectorFactory.decideConstraintMatchPolicy(phaseEnvironmentMode))
                .build()) {
            assertThat(scoreDirector.getConstraintMatchPolicy()).isEqualTo(ConstraintMatchPolicy.ENABLED);
        }
    }

    @Test
    void constraintMatchEnabledByMetric() {
        var scoreDirectorFactory = buildTestdataScoreDirectorFactory(SolverMetric.CONSTRAINT_MATCH_TOTAL_BEST_SCORE);
        assertThat(scoreDirectorFactory.decideConstraintMatchPolicy(EnvironmentMode.NO_ASSERT))
                .isEqualTo(ConstraintMatchPolicy.ENABLED);
    }

    @Test
    void constraintMatchNotEnabledByOtherMetric() {
        var scoreDirectorFactory = buildTestdataScoreDirectorFactory(SolverMetric.BEST_SCORE);
        assertThat(scoreDirectorFactory.decideConstraintMatchPolicy(EnvironmentMode.NO_ASSERT))
                .isEqualTo(ConstraintMatchPolicy.DISABLED);
    }

    private static DelegateScoreDirectorFactory<TestdataSolution, SimpleScore>
            buildTestdataScoreDirectorFactory(SolverMetric solverMetric) {
        var solverConfig = new SolverConfig()
                .withScoreDirectorFactory(constraintStreamConfig())
                .withMonitoringConfig(new MonitoringConfig().withSolverMetricList(List.of(solverMetric)));
        return new DelegateScoreDirectorFactory<>(solverConfig, TestdataSolution.buildSolutionDescriptor(),
                EnvironmentMode.NO_ASSERT);
    }

    // ************************************************************************
    // Configuration shared by all delegates
    // ************************************************************************

    @Test
    void incrementalScoreCalculatorWithCustomProperties() {
        var config = incrementalConfig();
        var customProperties = new HashMap<String, String>();
        customProperties.put("stringProperty", "string 1");
        customProperties.put("intProperty", "7");
        config.setIncrementalScoreCalculatorCustomProperties(customProperties);

        var scoreDirectorFactory =
                (IncrementalScoreDirectorFactory<TestdataSolution, SimpleScore>) buildTestdataScoreDirectorFactory(config)
                        .getDelegate();
        try (IncrementalScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                scoreDirectorFactory.buildScoreDirector()) {
            var scoreCalculator =
                    (TestCustomPropertiesIncrementalScoreCalculator) scoreDirector.getIncrementalScoreCalculator();
            assertThat(scoreCalculator.getStringProperty()).isEqualTo("string 1");
            assertThat(scoreCalculator.getIntProperty()).isEqualTo(7);
        }
    }

    @Test
    void initializingScoreTrendFromConfig() {
        var scoreDirectorFactory = buildTestdataScoreDirectorFactory(incrementalConfig()
                .withInitializingScoreTrend("ONLY_DOWN"));
        assertThat(scoreDirectorFactory.getInitializingScoreTrend())
                .isEqualTo(InitializingScoreTrend.parseTrend("ONLY_DOWN", 1));
        // The trend is set on the delegate, which is where the solver reads it from.
        assertThat(scoreDirectorFactory.getDelegate().getInitializingScoreTrend())
                .isSameAs(scoreDirectorFactory.getInitializingScoreTrend());
    }

    @Test
    void initializingScoreTrendDefaultsToAny() {
        assertThat(buildTestdataScoreDirectorFactory(incrementalConfig()).getInitializingScoreTrend())
                .isEqualTo(InitializingScoreTrend.parseTrend("ANY", 1));
    }

    @Test
    void buildWithAssertionScoreDirectorFactory() {
        var config = incrementalConfig()
                .withAssertionScoreDirectorFactory(incrementalConfig());

        var scoreDirectorFactory = (AbstractScoreDirectorFactory<TestdataSolution, ?, ?>) buildTestdataScoreDirectorFactory(
                config, EnvironmentMode.STEP_ASSERT).getDelegate();

        var assertionScoreDirectorFactory = scoreDirectorFactory.getAssertionScoreDirectorFactory();
        // The assertion factory is the delegate of its own DelegateScoreDirectorFactory,
        // as the code reading it expects a concrete factory.
        assertThat(assertionScoreDirectorFactory).isExactlyInstanceOf(IncrementalScoreDirectorFactory.class);
        try (IncrementalScoreDirector<TestdataSolution, SimpleScore> assertionScoreDirector =
                ((IncrementalScoreDirectorFactory<TestdataSolution, SimpleScore>) assertionScoreDirectorFactory)
                        .buildScoreDirector()) {
            // The assertion score director always runs in NON_REPRODUCIBLE, regardless of the requested mode.
            assertThat(assertionScoreDirector.environmentMode).isEqualTo(EnvironmentMode.NON_REPRODUCIBLE);
            var assertionScoreCalculator = assertionScoreDirector.getIncrementalScoreCalculator();
            assertThat(assertionScoreCalculator).isExactlyInstanceOf(TestCustomPropertiesIncrementalScoreCalculator.class);
        }
    }

    @Test
    void nestedAssertionScoreDirectorFactory_throwsException() {
        var config = incrementalConfig()
                .withAssertionScoreDirectorFactory(incrementalConfig()
                        .withAssertionScoreDirectorFactory(incrementalConfig()));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> buildTestdataScoreDirectorFactory(config, EnvironmentMode.STEP_ASSERT))
                .withMessageContaining("cannot have a non-null assertionScoreDirectorFactory");
    }

    @Test
    void assertionScoreDirectorFactoryInLenientEnvironmentMode_throwsException() {
        var config = incrementalConfig()
                .withAssertionScoreDirectorFactory(incrementalConfig());
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> buildTestdataScoreDirectorFactory(config, EnvironmentMode.PHASE_ASSERT))
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
