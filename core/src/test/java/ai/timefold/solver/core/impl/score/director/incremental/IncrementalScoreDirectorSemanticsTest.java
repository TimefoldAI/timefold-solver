package ai.timefold.solver.core.impl.score.director.incremental;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorSemanticsTest;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryFactory;
import ai.timefold.solver.core.impl.testdata.domain.constraintconfiguration.TestdataConstraintConfigurationSolution;
import ai.timefold.solver.core.impl.testdata.domain.constraintconfiguration.TestdataConstraintWeighIncrementalScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.TestdataPinnedListIncrementalScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.TestdataPinnedListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListIncrementalScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListSolution;

final class IncrementalScoreDirectorSemanticsTest extends AbstractScoreDirectorSemanticsTest {

    @Override
    protected ScoreDirectorFactory<TestdataConstraintConfigurationSolution, SimpleScore>
            buildScoreDirectorFactoryWithConstraintConfiguration(
                    SolutionDescriptor<TestdataConstraintConfigurationSolution> solutionDescriptor) {
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withIncrementalScoreCalculatorClass(TestdataConstraintWeighIncrementalScoreCalculator.class);
        var scoreDirectorFactoryFactory = new ScoreDirectorFactoryFactory<TestdataConstraintConfigurationSolution, SimpleScore>(
                scoreDirectorFactoryConfig);
        return scoreDirectorFactoryFactory.buildScoreDirectorFactory(EnvironmentMode.PHASE_ASSERT, solutionDescriptor);
    }

    @Override
    protected ScoreDirectorFactory<TestdataPinnedListSolution, SimpleScore>
            buildScoreDirectorFactoryWithListVariableEntityPin(
                    SolutionDescriptor<TestdataPinnedListSolution> solutionDescriptor) {
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withIncrementalScoreCalculatorClass(TestdataPinnedListIncrementalScoreCalculator.class);
        var scoreDirectorFactoryFactory =
                new ScoreDirectorFactoryFactory<TestdataPinnedListSolution, SimpleScore>(scoreDirectorFactoryConfig);
        return scoreDirectorFactoryFactory.buildScoreDirectorFactory(EnvironmentMode.PHASE_ASSERT, solutionDescriptor);
    }

    @Override
    protected ScoreDirectorFactory<TestdataPinnedWithIndexListSolution, SimpleScore>
            buildScoreDirectorFactoryWithListVariablePinIndex(
                    SolutionDescriptor<TestdataPinnedWithIndexListSolution> solutionDescriptor) {
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withIncrementalScoreCalculatorClass(TestdataPinnedWithIndexListIncrementalScoreCalculator.class);
        var scoreDirectorFactoryFactory =
                new ScoreDirectorFactoryFactory<TestdataPinnedWithIndexListSolution, SimpleScore>(scoreDirectorFactoryConfig);
        return scoreDirectorFactoryFactory.buildScoreDirectorFactory(EnvironmentMode.PHASE_ASSERT, solutionDescriptor);
    }

}
