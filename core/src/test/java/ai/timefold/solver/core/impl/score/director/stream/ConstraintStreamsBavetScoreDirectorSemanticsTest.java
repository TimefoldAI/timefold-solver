package ai.timefold.solver.core.impl.score.director.stream;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorSemanticsTest;
import ai.timefold.solver.core.impl.score.director.DelegateScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;
import ai.timefold.solver.core.testdomain.constraintweightoverrides.TestdataConstraintWeightOverridesConstraintProvider;
import ai.timefold.solver.core.testdomain.constraintweightoverrides.TestdataConstraintWeightOverridesSolution;
import ai.timefold.solver.core.testdomain.list.pinned.TestdataPinnedListConstraintProvider;
import ai.timefold.solver.core.testdomain.list.pinned.TestdataPinnedListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListConstraintProvider;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListSolution;

final class ConstraintStreamsBavetScoreDirectorSemanticsTest extends AbstractScoreDirectorSemanticsTest {

    @Override
    protected ScoreDirectorFactory<TestdataConstraintWeightOverridesSolution, SimpleScore>
            buildScoreDirectorFactoryWithConstraintConfiguration(
                    SolutionDescriptor<TestdataConstraintWeightOverridesSolution> solutionDescriptor) {
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataConstraintWeightOverridesConstraintProvider.class);
        return new DelegateScoreDirectorFactory<>(scoreDirectorFactoryConfig, solutionDescriptor,
                EnvironmentMode.PHASE_ASSERT);
    }

    @Override
    protected ScoreDirectorFactory<TestdataPinnedListSolution, SimpleScore>
            buildScoreDirectorFactoryWithListVariableEntityPin(
                    SolutionDescriptor<TestdataPinnedListSolution> solutionDescriptor) {
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataPinnedListConstraintProvider.class);
        return new DelegateScoreDirectorFactory<>(scoreDirectorFactoryConfig, solutionDescriptor,
                EnvironmentMode.PHASE_ASSERT);
    }

    @Override
    protected ScoreDirectorFactory<TestdataPinnedWithIndexListSolution, SimpleScore>
            buildScoreDirectorFactoryWithListVariablePinIndex(
                    SolutionDescriptor<TestdataPinnedWithIndexListSolution> solutionDescriptor) {
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataPinnedWithIndexListConstraintProvider.class);
        return new DelegateScoreDirectorFactory<>(scoreDirectorFactoryConfig, solutionDescriptor,
                EnvironmentMode.PHASE_ASSERT);
    }

}
