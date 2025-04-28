package ai.timefold.solver.quarkus.testdomain.gizmo;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

import org.jspecify.annotations.NonNull;

public class DummyVariableListener implements VariableListener {

    @Override
    public void beforeEntityAdded(@NonNull ScoreDirector scoreDirector, @NonNull Object o) {
        // Ignore
    }

    @Override
    public void afterEntityAdded(@NonNull ScoreDirector scoreDirector, @NonNull Object o) {
        // Ignore
    }

    @Override
    public void beforeVariableChanged(@NonNull ScoreDirector scoreDirector, @NonNull Object o) {
        // Ignore
    }

    @Override
    public void afterVariableChanged(@NonNull ScoreDirector scoreDirector, @NonNull Object o) {
        // Ignore
    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector scoreDirector, @NonNull Object o) {
        // Ignore
    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector scoreDirector, @NonNull Object o) {
        // Ignore
    }
}
