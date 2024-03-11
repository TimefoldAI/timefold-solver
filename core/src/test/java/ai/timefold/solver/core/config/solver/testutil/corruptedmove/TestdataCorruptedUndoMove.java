package ai.timefold.solver.core.config.solver.testutil.corruptedmove;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

public class TestdataCorruptedUndoMove extends AbstractTestdataMove {

    public TestdataCorruptedUndoMove(TestdataEntity entity, TestdataValue toValue) {
        super(entity, toValue);
    }

    @Override
    protected AbstractMove<TestdataSolution> createUndoMove(ScoreDirector<TestdataSolution> scoreDirector) {
        // Corrupts the undo move by not undo-ing the value
        return new TestdataCorruptedUndoMove(entity, toValue);
    }
}
