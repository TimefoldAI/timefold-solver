package ai.timefold.solver.constraint.drl;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorSemanticsTest;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryFactory;
import ai.timefold.solver.core.impl.testdata.domain.constraintconfiguration.TestdataConstraintConfigurationSolution;

final class DrlScoreDirectorSemanticsTest extends AbstractScoreDirectorSemanticsTest {

    @Override
    protected InnerScoreDirectorFactory<TestdataConstraintConfigurationSolution, SimpleScore>
            buildInnerScoreDirectorFactory(SolutionDescriptor<TestdataConstraintConfigurationSolution> solutionDescriptor) {
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withScoreDrls("ai/timefold/solver/constraint/drl/scoreDirectorSemanticsDroolsConstraints.drl",
                        "ai/timefold/solver/constraint/drl/scoreDirectorSemanticsDroolsConstraints2.drl");
        ScoreDirectorFactoryFactory<TestdataConstraintConfigurationSolution, SimpleScore> scoreDirectorFactoryFactory =
                new ScoreDirectorFactoryFactory<>(scoreDirectorFactoryConfig);
        return scoreDirectorFactoryFactory.buildScoreDirectorFactory(getClass().getClassLoader(),
                EnvironmentMode.REPRODUCIBLE, solutionDescriptor);
    }

}
