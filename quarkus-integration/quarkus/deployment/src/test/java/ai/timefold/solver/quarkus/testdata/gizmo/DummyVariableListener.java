package ai.timefold.solver.quarkus.testdata.gizmo;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

public class DummyVariableListener implements VariableListener {

    @Override
    public void beforeEntityAdded(ScoreDirector scoreDirector, Object o) {

    }

    @Override
    public void afterEntityAdded(ScoreDirector scoreDirector, Object o) {

    }

    @Override
    public void beforeVariableChanged(ScoreDirector scoreDirector, Object o) {

    }

    @Override
    public void afterVariableChanged(ScoreDirector scoreDirector, Object o) {

    }

    @Override
    public void beforeEntityRemoved(ScoreDirector scoreDirector, Object o) {

    }

    @Override
    public void afterEntityRemoved(ScoreDirector scoreDirector, Object o) {

    }
}
