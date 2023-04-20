package ai.timefold.solver.constraint.streams.bavet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ai.timefold.solver.constraint.streams.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryFactory;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class ScoreDirectorFactoryFactoryTest {

    @Test
    void constraintStreamsDroolsWithAlphaNetworkCompilationEnabledNoDrools_throws() {
        ScoreDirectorFactoryConfig config = new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataConstraintProvider.class)
                .withConstraintStreamImplType(ConstraintStreamImplType.BAVET)
                .withDroolsAlphaNetworkCompilationEnabled(true);
        ScoreDirectorFactoryFactory<TestdataSolution, SimpleScore> factoryFactory = new ScoreDirectorFactoryFactory<>(config);
        assertThatCode(() -> factoryFactory.buildScoreDirectorFactory(getClass().getClassLoader(),
                EnvironmentMode.FAST_ASSERT,
                TestdataSolution.buildSolutionDescriptor()))
                .hasMessage("If there is no scoreDrl (null), scoreDrlFile (null) or constraintProviderClass "
                        + "(class ai.timefold.solver.constraint.streams.bavet.ScoreDirectorFactoryFactoryTest$TestdataConstraintProvider)"
                        + " with DROOLS impl type (BAVET), there can be no droolsAlphaNetworkCompilationEnabled (true) either.");
    }

    @Test
    void constraintStreamsBavet() {
        ScoreDirectorFactoryConfig config = new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataConstraintProvider.class)
                .withConstraintStreamImplType(ConstraintStreamImplType.BAVET);
        AbstractConstraintStreamScoreDirectorFactory<TestdataSolution, SimpleScore> scoreDirectorFactory =
                (AbstractConstraintStreamScoreDirectorFactory<TestdataSolution, SimpleScore>) new BavetConstraintStreamScoreDirectorFactoryService<TestdataSolution, SimpleScore>()
                        .buildScoreDirectorFactory(null, TestdataSolution.buildSolutionDescriptor(), config,
                                EnvironmentMode.REPRODUCIBLE)
                        .get();
        assertThat(scoreDirectorFactory).isInstanceOf(BavetConstraintStreamScoreDirectorFactory.class);
    }

    public static class TestdataConstraintProvider implements ConstraintProvider {
        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
            return new Constraint[0];
        }
    }
}
