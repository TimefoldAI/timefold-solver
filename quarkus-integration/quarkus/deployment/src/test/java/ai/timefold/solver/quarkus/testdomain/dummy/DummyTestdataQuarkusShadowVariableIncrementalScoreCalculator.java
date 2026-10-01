package ai.timefold.solver.quarkus.testdomain.dummy;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class DummyTestdataQuarkusShadowVariableIncrementalScoreCalculator
        implements IncrementalScoreCalculator<Object, SimpleScore> {

    @Override
    public void resetWorkingSolution(Object workingSolution) {
        // Ignore
    }

    @Override
    public void beforeVariableChanged(Object entity, String variableName) {
        // Ignore
    }

    @Override
    public void afterVariableChanged(Object entity, String variableName) {
        // Ignore
    }

    @Override
    public SimpleScore calculateScore() {
        return null;
    }
}
