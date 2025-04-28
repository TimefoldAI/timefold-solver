package ai.timefold.solver.quarkus.testdomain.dummy;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;

import org.jspecify.annotations.NonNull;

public class DummyTestdataQuarkusShadowVariableIncrementalScoreCalculator implements IncrementalScoreCalculator {

    @Override
    public void resetWorkingSolution(@NonNull Object workingSolution) {
        // Ignore
    }

    @Override
    public void beforeEntityAdded(@NonNull Object entity) {
        // Ignore
    }

    @Override
    public void afterEntityAdded(@NonNull Object entity) {
        // Ignore
    }

    @Override
    public void beforeVariableChanged(@NonNull Object entity, @NonNull String variableName) {
        // Ignore
    }

    @Override
    public void afterVariableChanged(@NonNull Object entity, @NonNull String variableName) {
        // Ignore
    }

    @Override
    public void beforeEntityRemoved(@NonNull Object entity) {
        // Ignore
    }

    @Override
    public void afterEntityRemoved(@NonNull Object entity) {
        // Ignore
    }

    @Override
    public @NonNull Score calculateScore() {
        return null;
    }
}
