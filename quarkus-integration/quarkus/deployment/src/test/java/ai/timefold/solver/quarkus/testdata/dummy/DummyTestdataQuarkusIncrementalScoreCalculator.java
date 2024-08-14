package ai.timefold.solver.quarkus.testdata.dummy;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;

import org.jspecify.annotations.NonNull;

public class DummyTestdataQuarkusIncrementalScoreCalculator implements IncrementalScoreCalculator {

    @Override
    public void resetWorkingSolution(@NonNull Object workingSolution) {

    }

    @Override
    public void beforeEntityAdded(@NonNull Object entity) {

    }

    @Override
    public void afterEntityAdded(@NonNull Object entity) {

    }

    @Override
    public void beforeVariableChanged(@NonNull Object entity, @NonNull String variableName) {

    }

    @Override
    public void afterVariableChanged(@NonNull Object entity, @NonNull String variableName) {

    }

    @Override
    public void beforeEntityRemoved(@NonNull Object entity) {

    }

    @Override
    public void afterEntityRemoved(@NonNull Object entity) {

    }

    @Override
    public @NonNull Score calculateScore() {
        return null;
    }
}
