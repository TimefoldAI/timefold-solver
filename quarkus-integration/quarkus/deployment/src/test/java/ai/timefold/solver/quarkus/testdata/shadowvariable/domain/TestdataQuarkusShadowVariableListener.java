package ai.timefold.solver.quarkus.testdata.shadowvariable.domain;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

public class TestdataQuarkusShadowVariableListener
        implements VariableListener<TestdataQuarkusShadowVariableSolution, TestdataQuarkusShadowVariableEntity> {
    @Override
    public void beforeEntityAdded(ScoreDirector<TestdataQuarkusShadowVariableSolution> scoreDirector,
            TestdataQuarkusShadowVariableEntity testdataQuarkusShadowVariableEntity) {

    }

    @Override
    public void afterEntityAdded(ScoreDirector<TestdataQuarkusShadowVariableSolution> scoreDirector,
            TestdataQuarkusShadowVariableEntity testdataQuarkusShadowVariableEntity) {
        update(scoreDirector, testdataQuarkusShadowVariableEntity);
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<TestdataQuarkusShadowVariableSolution> scoreDirector,
            TestdataQuarkusShadowVariableEntity testdataQuarkusShadowVariableEntity) {

    }

    @Override
    public void afterEntityRemoved(ScoreDirector<TestdataQuarkusShadowVariableSolution> scoreDirector,
            TestdataQuarkusShadowVariableEntity testdataQuarkusShadowVariableEntity) {

    }

    @Override
    public void beforeVariableChanged(ScoreDirector<TestdataQuarkusShadowVariableSolution> scoreDirector,
            TestdataQuarkusShadowVariableEntity testdataQuarkusShadowVariableEntity) {

    }

    @Override
    public void afterVariableChanged(ScoreDirector<TestdataQuarkusShadowVariableSolution> scoreDirector,
            TestdataQuarkusShadowVariableEntity testdataQuarkusShadowVariableEntity) {
        update(scoreDirector, testdataQuarkusShadowVariableEntity);
    }

    void update(ScoreDirector<TestdataQuarkusShadowVariableSolution> scoreDirector,
            TestdataQuarkusShadowVariableEntity entity) {
        scoreDirector.beforeVariableChanged(entity, "value1AndValue2");
        entity.setValue1AndValue2(entity.getValue1() + entity.getValue2());
        scoreDirector.afterVariableChanged(entity, "value1AndValue2");
    }
}
