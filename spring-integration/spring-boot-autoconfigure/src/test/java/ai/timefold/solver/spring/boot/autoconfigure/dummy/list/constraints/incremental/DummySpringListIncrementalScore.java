package ai.timefold.solver.spring.boot.autoconfigure.dummy.list.constraints.incremental;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class DummySpringListIncrementalScore
        implements IncrementalScoreCalculator<Object, SimpleScore> {

    @Override
    public void resetWorkingSolution(Object workingSolution) {

    }

    @Override
    public void beforeVariableChanged(Object entity, String variableName) {

    }

    @Override
    public void afterVariableChanged(Object entity, String variableName) {

    }

    @Override
    public SimpleScore calculateScore() {
        return null;
    }
}
