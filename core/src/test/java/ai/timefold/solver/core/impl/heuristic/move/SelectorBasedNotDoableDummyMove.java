package ai.timefold.solver.core.impl.heuristic.move;

import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class SelectorBasedNotDoableDummyMove extends SelectorBasedDummyMove {

    public SelectorBasedNotDoableDummyMove() {
    }

    public SelectorBasedNotDoableDummyMove(String code) {
        super(code);
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<TestdataSolution> scoreDirector) {
        return false;
    }

}
