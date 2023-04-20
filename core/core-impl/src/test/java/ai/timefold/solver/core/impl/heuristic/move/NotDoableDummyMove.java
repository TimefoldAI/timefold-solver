package ai.timefold.solver.core.impl.heuristic.move;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

public class NotDoableDummyMove extends DummyMove {

    public NotDoableDummyMove() {
    }

    public NotDoableDummyMove(String code) {
        super(code);
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<TestdataSolution> scoreDirector) {
        return false;
    }

}
