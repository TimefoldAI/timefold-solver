package ai.timefold.solver.quarkus.testdata.gizmo;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

import org.jspecify.annotations.NonNull;

public class DummyVariableListener implements VariableListener {

    @Override
    public void beforeEntityAdded(@NonNull ScoreDirector scoreDirector, @NonNull Object o) {

    }

    @Override
    public void afterEntityAdded(@NonNull ScoreDirector scoreDirector, @NonNull Object o) {

    }

    @Override
    public void beforeVariableChanged(@NonNull ScoreDirector scoreDirector, @NonNull Object o) {

    }

    @Override
    public void afterVariableChanged(@NonNull ScoreDirector scoreDirector, @NonNull Object o) {

    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector scoreDirector, @NonNull Object o) {

    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector scoreDirector, @NonNull Object o) {

    }
}
