package ai.timefold.solver.core.testdomain;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

import org.jspecify.annotations.NonNull;

public abstract class DummyVariableListener<Solution_, Entity_> implements VariableListener<Solution_, Entity_> {

    @Override
    public void beforeEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Entity_ entity_) {
        // Nothing to do.
    }

    @Override
    public void afterEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Entity_ entity_) {
        // Nothing to do.
    }

    @Override
    public void beforeVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Entity_ entity_) {
        // Nothing to do.
    }

    @Override
    public void afterVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Entity_ entity_) {
        // Nothing to do.
    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Entity_ entity_) {
        // Nothing to do.
    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Entity_ entity_) {
        // Nothing to do.
    }
}
