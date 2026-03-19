package ai.timefold.solver.core.impl.score.director;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

class ScoreDirectorFactoryFactoryTest {

    @Test
    void multipleScoreCalculations_throwsException() {
        var config = new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataConstraintProvider.class)
                .withEasyScoreCalculatorClass(TestCustomPropertiesEasyScoreCalculator.class);
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> buildTestdataScoreDirectoryFactory(config))
                .withMessageContaining("scoreDirectorFactory")
                .withMessageContaining("together");
    }

    private ScoreDirectorFactory<TestdataSolution, SimpleScore>
            buildTestdataScoreDirectoryFactory(ScoreDirectorFactoryConfig config, EnvironmentMode environmentMode) {
        return new ScoreDirectorFactoryFactory<TestdataSolution, SimpleScore>(config)
                .buildScoreDirectorFactory(environmentMode, TestdataSolution.buildSolutionDescriptor());
    }

    private ScoreDirectorFactory<TestdataSolution, SimpleScore>
            buildTestdataScoreDirectoryFactory(ScoreDirectorFactoryConfig config) {
        return buildTestdataScoreDirectoryFactory(config, EnvironmentMode.PHASE_ASSERT);
    }

    @Test
    void constraintStreamsBavet() {
        var config = new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataConstraintProvider.class);
        var scoreDirectorFactory =
                BavetConstraintStreamScoreDirectorFactory.buildScoreDirectorFactory(TestdataSolution.buildSolutionDescriptor(),
                        config, EnvironmentMode.PHASE_ASSERT);
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
        public @NonNull SimpleScore calculateScore(@NonNull TestdataSolution testdataSolution) {
            return SimpleScore.ZERO;
        }
    }

    public static class TestdataConstraintProvider implements ConstraintProvider {
        @Override
        public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
            return new Constraint[0];
        }
    }

}
