package ai.timefold.solver.constraint.drl;

import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.impl.solver.AbstractSolutionManagerTest;

final class DrlSolutionManagerTest extends AbstractSolutionManagerTest {

    @Override
    protected ScoreDirectorFactoryConfig buildScoreDirectorFactoryConfig() {
        return new ScoreDirectorFactoryConfig()
                .withScoreDrls("ai/timefold/solver/constraint/drl/solutionManagerDroolsConstraints.drl");
    }

}
