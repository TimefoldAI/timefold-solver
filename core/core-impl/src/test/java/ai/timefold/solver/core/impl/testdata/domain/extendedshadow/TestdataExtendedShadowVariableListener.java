package ai.timefold.solver.core.impl.testdata.domain.extendedshadow;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

public class TestdataExtendedShadowVariableListener
        implements VariableListener<TestdataExtendedShadowSolution, TestdataExtendedShadowEntity> {

    @Override
    public void beforeEntityAdded(ScoreDirector<TestdataExtendedShadowSolution> scoreDirector,
            TestdataExtendedShadowEntity entity) {
    }

    @Override
    public void afterEntityAdded(ScoreDirector<TestdataExtendedShadowSolution> scoreDirector,
            TestdataExtendedShadowEntity entity) {
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<TestdataExtendedShadowSolution> scoreDirector,
            TestdataExtendedShadowEntity entity) {
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<TestdataExtendedShadowSolution> scoreDirector,
            TestdataExtendedShadowEntity entity) {
    }

    @Override
    public void beforeVariableChanged(ScoreDirector<TestdataExtendedShadowSolution> scoreDirector,
            TestdataExtendedShadowEntity entity) {
    }

    @Override
    public void afterVariableChanged(ScoreDirector<TestdataExtendedShadowSolution> scoreDirector,
            TestdataExtendedShadowEntity entity) {
    }
}
