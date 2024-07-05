package ai.timefold.solver.quarkus.benchmark.it.domain;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

public class StringLengthVariableListener
        implements VariableListener<TestdataStringLengthShadowSolution, TestdataListValueShadowEntity> {

    @Override
    public void beforeEntityAdded(ScoreDirector<TestdataStringLengthShadowSolution> scoreDirector,
            TestdataListValueShadowEntity entity) {
        /* Nothing to do */
    }

    @Override
    public void afterEntityAdded(ScoreDirector<TestdataStringLengthShadowSolution> scoreDirector,
            TestdataListValueShadowEntity entity) {
        /* Nothing to do */
    }

    @Override
    public void beforeVariableChanged(ScoreDirector<TestdataStringLengthShadowSolution> scoreDirector,
            TestdataListValueShadowEntity entity) {
        /* Nothing to do */
    }

    @Override
    public void afterVariableChanged(ScoreDirector<TestdataStringLengthShadowSolution> scoreDirector,
            TestdataListValueShadowEntity entity) {
        int oldLength = (entity.getLength() != null) ? entity.getLength() : 0;
        int newLength =
                entity.getEntity() != null
                        ? entity.getEntity().getValues().stream().map(TestdataListValueShadowEntity::getValue)
                                .mapToInt(StringLengthVariableListener::getLength).sum()
                        : 0;
        if (oldLength != newLength) {
            scoreDirector.beforeVariableChanged(entity, "length");
            entity.setLength(newLength);
            scoreDirector.afterVariableChanged(entity, "length");
        }
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<TestdataStringLengthShadowSolution> scoreDirector,
            TestdataListValueShadowEntity entity) {
        /* Nothing to do */
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<TestdataStringLengthShadowSolution> scoreDirector,
            TestdataListValueShadowEntity entity) {
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
