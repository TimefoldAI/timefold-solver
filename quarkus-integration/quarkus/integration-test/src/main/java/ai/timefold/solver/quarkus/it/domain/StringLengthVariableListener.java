package ai.timefold.solver.quarkus.it.domain;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

import org.jspecify.annotations.NonNull;

public class StringLengthVariableListener
        implements VariableListener<TestdataStringLengthShadowSolution, TestdataStringLengthShadowEntity> {

    @Override
    public void beforeEntityAdded(@NonNull ScoreDirector<TestdataStringLengthShadowSolution> scoreDirector,
            @NonNull TestdataStringLengthShadowEntity entity) {
        /* Nothing to do */
    }

    @Override
    public void afterEntityAdded(@NonNull ScoreDirector<TestdataStringLengthShadowSolution> scoreDirector,
            @NonNull TestdataStringLengthShadowEntity entity) {
        /* Nothing to do */
    }

    @Override
    public void beforeVariableChanged(@NonNull ScoreDirector<TestdataStringLengthShadowSolution> scoreDirector,
            @NonNull TestdataStringLengthShadowEntity entity) {
        /* Nothing to do */
    }

    @Override
    public void afterVariableChanged(@NonNull ScoreDirector<TestdataStringLengthShadowSolution> scoreDirector,
            @NonNull TestdataStringLengthShadowEntity entity) {
        int oldLength = (entity.getLength() != null) ? entity.getLength() : 0;
        int newLength = getLength(entity.getValue());
        if (oldLength != newLength) {
            scoreDirector.beforeVariableChanged(entity, "length");
            entity.setLength(getLength(entity.getValue()));
            scoreDirector.afterVariableChanged(entity, "length");
        }
    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector<TestdataStringLengthShadowSolution> scoreDirector,
            @NonNull TestdataStringLengthShadowEntity entity) {
        /* Nothing to do */
    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector<TestdataStringLengthShadowSolution> scoreDirector,
            @NonNull TestdataStringLengthShadowEntity entity) {
        /* Nothing to do */
    }

    private static int getLength(String value) {
        if (value != null) {
            return value.length();
        } else {
            return 0;
        }
    }
}
