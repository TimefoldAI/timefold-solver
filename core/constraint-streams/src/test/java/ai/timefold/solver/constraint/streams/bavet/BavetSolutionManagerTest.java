package ai.timefold.solver.constraint.streams.bavet;

import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.impl.solver.AbstractSolutionManagerTest;
import ai.timefold.solver.core.impl.testdata.domain.TestdataConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned_values.TestdataAllowsUnassignedValuesListConstraintProvider;

final class BavetSolutionManagerTest extends AbstractSolutionManagerTest {

    @Override
    protected ScoreDirectorFactoryConfig buildScoreDirectorFactoryConfig() {
        return new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataConstraintProvider.class)
                .withConstraintStreamImplType(ConstraintStreamImplType.BAVET);
    }

    @Override
    protected ScoreDirectorFactoryConfig buildUnassignedScoreDirectorFactoryConfig() {
        return new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(TestdataAllowsUnassignedValuesListConstraintProvider.class)
                .withConstraintStreamImplType(ConstraintStreamImplType.BAVET);
    }

}
