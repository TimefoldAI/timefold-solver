package ai.timefold.solver.quarkus.testdata.shadowvariable.domain;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

import org.jspecify.annotations.NonNull;

public class TestdataQuarkusShadowVariableListener
        implements VariableListener<TestdataQuarkusShadowVariableSolution, TestdataQuarkusShadowVariableEntity> {
    @Override
    public void beforeEntityAdded(@NonNull ScoreDirector<TestdataQuarkusShadowVariableSolution> scoreDirector,
            @NonNull TestdataQuarkusShadowVariableEntity testdataQuarkusShadowVariableEntity) {

    }

    @Override
    public void afterEntityAdded(@NonNull ScoreDirector<TestdataQuarkusShadowVariableSolution> scoreDirector,
            @NonNull TestdataQuarkusShadowVariableEntity testdataQuarkusShadowVariableEntity) {
        update(scoreDirector, testdataQuarkusShadowVariableEntity);
    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector<TestdataQuarkusShadowVariableSolution> scoreDirector,
            @NonNull TestdataQuarkusShadowVariableEntity testdataQuarkusShadowVariableEntity) {

    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector<TestdataQuarkusShadowVariableSolution> scoreDirector,
            @NonNull TestdataQuarkusShadowVariableEntity testdataQuarkusShadowVariableEntity) {

    }

    @Override
    public void beforeVariableChanged(@NonNull ScoreDirector<TestdataQuarkusShadowVariableSolution> scoreDirector,
            @NonNull TestdataQuarkusShadowVariableEntity testdataQuarkusShadowVariableEntity) {

    }

    @Override
    public void afterVariableChanged(@NonNull ScoreDirector<TestdataQuarkusShadowVariableSolution> scoreDirector,
            @NonNull TestdataQuarkusShadowVariableEntity testdataQuarkusShadowVariableEntity) {
        update(scoreDirector, testdataQuarkusShadowVariableEntity);
    }

    void update(ScoreDirector<TestdataQuarkusShadowVariableSolution> scoreDirector,
            TestdataQuarkusShadowVariableEntity entity) {
        scoreDirector.beforeVariableChanged(entity, "value1AndValue2");
        entity.setValue1AndValue2(entity.getValue1() + entity.getValue2());
        scoreDirector.afterVariableChanged(entity, "value1AndValue2");
    }
}
