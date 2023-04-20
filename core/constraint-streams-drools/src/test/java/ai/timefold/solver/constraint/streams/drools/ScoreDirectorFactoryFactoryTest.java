package ai.timefold.solver.constraint.streams.drools;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirectorFactory;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class ScoreDirectorFactoryFactoryTest {

    @Test
    void constraintStreamsDroolsWithAlphaNetworkCompilationEnabled() {
        ScoreDirectorFactoryConfig config = new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataConstraintProvider.class)
                .withConstraintStreamImplType(ConstraintStreamImplType.DROOLS)
                .withDroolsAlphaNetworkCompilationEnabled(true);
        InnerScoreDirectorFactory<TestdataSolution, SimpleScore> uncastScoreDirectorFactory =
                new DroolsConstraintStreamScoreDirectorFactoryService<TestdataSolution, SimpleScore>()
                        .buildScoreDirectorFactory(null, TestdataSolution.buildSolutionDescriptor(), config,
                                EnvironmentMode.REPRODUCIBLE)
                        .get();
        assertThat(uncastScoreDirectorFactory).isInstanceOf(DroolsConstraintStreamScoreDirectorFactory.class);
        DroolsConstraintStreamScoreDirectorFactory<TestdataSolution, SimpleScore> scoreDirectorFactory =
                (DroolsConstraintStreamScoreDirectorFactory<TestdataSolution, SimpleScore>) uncastScoreDirectorFactory;
        assertThat(scoreDirectorFactory.isDroolsAlphaNetworkCompilationEnabled()).isTrue();
    }

    @Test
    void constraintStreamsDroolsWithAlphaNetworkCompilationDisabled() {
        ScoreDirectorFactoryConfig config = new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataConstraintProvider.class)
                .withConstraintStreamImplType(ConstraintStreamImplType.DROOLS)
                .withDroolsAlphaNetworkCompilationEnabled(false);
        InnerScoreDirectorFactory<TestdataSolution, SimpleScore> uncastScoreDirectorFactory =
                new DroolsConstraintStreamScoreDirectorFactoryService<TestdataSolution, SimpleScore>()
                        .buildScoreDirectorFactory(null, TestdataSolution.buildSolutionDescriptor(), config,
                                EnvironmentMode.REPRODUCIBLE)
                        .get();
        assertThat(uncastScoreDirectorFactory).isInstanceOf(DroolsConstraintStreamScoreDirectorFactory.class);
        DroolsConstraintStreamScoreDirectorFactory<TestdataSolution, SimpleScore> scoreDirectorFactory =
                (DroolsConstraintStreamScoreDirectorFactory<TestdataSolution, SimpleScore>) uncastScoreDirectorFactory;
        assertThat(scoreDirectorFactory.isDroolsAlphaNetworkCompilationEnabled()).isFalse();
    }

    public static class TestdataConstraintProvider implements ConstraintProvider {
        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
            return new Constraint[0];
        }
    }
}
