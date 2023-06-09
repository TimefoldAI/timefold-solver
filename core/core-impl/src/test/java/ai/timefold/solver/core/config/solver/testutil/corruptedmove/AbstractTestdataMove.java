package ai.timefold.solver.core.config.solver.testutil.corruptedmove;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

public abstract class AbstractTestdataMove extends AbstractMove<TestdataSolution> {

    TestdataEntity entity;
    TestdataValue toValue;

    AbstractTestdataMove(TestdataEntity entity, TestdataValue toValue) {
        this.entity = entity;
        this.toValue = toValue;
    }

    @Override
    public Collection<?> getPlanningEntities() {
        return Collections.singletonList(entity);
    }

    @Override
    public Collection<?> getPlanningValues() {
        return Collections.singletonList(toValue);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<TestdataSolution> scoreDirector) {
        scoreDirector.beforeVariableChanged(entity, "value");
        entity.setValue(toValue);
        scoreDirector.afterVariableChanged(entity, "value");
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<TestdataSolution> scoreDirector) {
        return !Objects.equals(entity.getValue(), toValue);
    }

    @Override
    public String toString() {
        return entity + " -> " + toValue;
    }
}
