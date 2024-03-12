package ai.timefold.solver.core.impl.score.director;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.HashMap;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.director.incremental.IncrementalScoreDirector;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class ScoreDirectorFactoryFactoryTest {

    @Test
    void incrementalScoreCalculatorWithCustomProperties() {
        ScoreDirectorFactoryConfig config = new ScoreDirectorFactoryConfig();
        config.setIncrementalScoreCalculatorClass(
                TestCustomPropertiesIncrementalScoreCalculator.class);
        HashMap<String, String> customProperties = new HashMap<>();
        customProperties.put("stringProperty", "string 1");
        customProperties.put("intProperty", "7");
        config.setIncrementalScoreCalculatorCustomProperties(customProperties);

        ScoreDirectorFactory<TestdataSolution> scoreDirectorFactory = buildTestdataScoreDirectoryFactory(config);
        IncrementalScoreDirector<TestdataSolution, ?> scoreDirector =
                (IncrementalScoreDirector<TestdataSolution, ?>) scoreDirectorFactory.buildScoreDirector();
        TestCustomPropertiesIncrementalScoreCalculator scoreCalculator =
                (TestCustomPropertiesIncrementalScoreCalculator) scoreDirector
                        .getIncrementalScoreCalculator();
        assertThat(scoreCalculator.getStringProperty()).isEqualTo("string 1");
        assertThat(scoreCalculator.getIntProperty()).isEqualTo(7);
    }

    @Test
    void buildWithAssertionScoreDirectorFactory() {
        ScoreDirectorFactoryConfig assertionScoreDirectorConfig = new ScoreDirectorFactoryConfig()
                .withIncrementalScoreCalculatorClass(TestCustomPropertiesIncrementalScoreCalculator.class);
        ScoreDirectorFactoryConfig config = new ScoreDirectorFactoryConfig()
                .withIncrementalScoreCalculatorClass(TestCustomPropertiesIncrementalScoreCalculator.class)
                .withAssertionScoreDirectorFactory(assertionScoreDirectorConfig);

        AbstractScoreDirectorFactory<TestdataSolution, ?> scoreDirectorFactory =
                (AbstractScoreDirectorFactory<TestdataSolution, ?>) buildTestdataScoreDirectoryFactory(config,
                        EnvironmentMode.FAST_ASSERT);

        ScoreDirectorFactory<TestdataSolution> assertionScoreDirectorFactory =
                scoreDirectorFactory.getAssertionScoreDirectorFactory();
        IncrementalScoreDirector<TestdataSolution, ?> assertionScoreDirector =
                (IncrementalScoreDirector<TestdataSolution, ?>) assertionScoreDirectorFactory.buildScoreDirector();
        IncrementalScoreCalculator<TestdataSolution, ?> assertionScoreCalculator =
                assertionScoreDirector.getIncrementalScoreCalculator();

        assertThat(assertionScoreCalculator).isExactlyInstanceOf(TestCustomPropertiesIncrementalScoreCalculator.class);
    }

    @Test
    void multipleScoreCalculations_throwsException() {
        ScoreDirectorFactoryConfig config = new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataConstraintProvider.class)
                .withEasyScoreCalculatorClass(TestCustomPropertiesEasyScoreCalculator.class)
                .withIncrementalScoreCalculatorClass(TestCustomPropertiesIncrementalScoreCalculator.class);
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> buildTestdataScoreDirectoryFactory(config))
                .withMessageContaining("scoreDirectorFactory")
                .withMessageContaining("together");
    }

    private <Score_ extends Score<Score_>> ScoreDirectorFactory<TestdataSolution> buildTestdataScoreDirectoryFactory(
            ScoreDirectorFactoryConfig config, EnvironmentMode environmentMode) {
        return new ScoreDirectorFactoryFactory<TestdataSolution, Score_>(config)
                .buildScoreDirectorFactory(environmentMode, TestdataSolution.buildSolutionDescriptor());
    }

    private ScoreDirectorFactory<TestdataSolution> buildTestdataScoreDirectoryFactory(ScoreDirectorFactoryConfig config) {
        return buildTestdataScoreDirectoryFactory(config, EnvironmentMode.REPRODUCIBLE);
    }

    @Test
    void constraintStreamsBavet() {
        var config = new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataConstraintProvider.class)
                .withConstraintStreamImplType(ConstraintStreamImplType.BAVET);
        var scoreDirectorFactory =
                BavetConstraintStreamScoreDirectorFactory.buildScoreDirectorFactory(TestdataSolution.buildSolutionDescriptor(),
                        config, EnvironmentMode.REPRODUCIBLE);
        assertThat(scoreDirectorFactory).isInstanceOf(BavetConstraintStreamScoreDirectorFactory.class);
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
        public SimpleScore calculateScore(TestdataSolution testdataSolution) {
            return SimpleScore.ZERO;
        }
    }

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
        }

        @Override
        public void beforeEntityAdded(Object entity) {
        }

        @Override
        public void afterEntityAdded(Object entity) {
        }

        @Override
        public void beforeVariableChanged(Object entity, String variableName) {
        }

        @Override
        public void afterVariableChanged(Object entity, String variableName) {
        }

        @Override
        public void beforeEntityRemoved(Object entity) {
        }

        @Override
        public void afterEntityRemoved(Object entity) {
        }

        @Override
        public SimpleScore calculateScore() {
            return SimpleScore.ZERO;
        }
    }

    public static class TestdataConstraintProvider implements ConstraintProvider {
        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
            return new Constraint[0];
        }
    }

}
