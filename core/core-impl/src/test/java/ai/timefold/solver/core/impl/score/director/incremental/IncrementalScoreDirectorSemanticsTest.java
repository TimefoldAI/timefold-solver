package ai.timefold.solver.core.impl.score.director.incremental;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorSemanticsTest;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryFactory;
import ai.timefold.solver.core.impl.testdata.domain.constraintconfiguration.TestdataConstraintConfigurationSolution;
import ai.timefold.solver.core.impl.testdata.domain.constraintconfiguration.TestdataConstraintWeighIncrementalScoreCalculator;

final class IncrementalScoreDirectorSemanticsTest extends AbstractScoreDirectorSemanticsTest {

    @Override
    protected InnerScoreDirectorFactory<TestdataConstraintConfigurationSolution, SimpleScore>
            buildInnerScoreDirectorFactory(SolutionDescriptor<TestdataConstraintConfigurationSolution> solutionDescriptor) {
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withIncrementalScoreCalculatorClass(TestdataConstraintWeighIncrementalScoreCalculator.class);
        ScoreDirectorFactoryFactory<TestdataConstraintConfigurationSolution, SimpleScore> scoreDirectorFactoryFactory =
                new ScoreDirectorFactoryFactory<>(scoreDirectorFactoryConfig);
        return scoreDirectorFactoryFactory.buildScoreDirectorFactory(getClass().getClassLoader(),
                EnvironmentMode.REPRODUCIBLE, solutionDescriptor);
    }

}
