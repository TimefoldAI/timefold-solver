package ai.timefold.solver.core.config.solver.testutil.corruptedmove;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

public class TestdataCorruptedEntityUndoMove extends AbstractTestdataMove {
    TestdataEntity undoEntity;

    public TestdataCorruptedEntityUndoMove(TestdataEntity entity, TestdataEntity undoEntity, TestdataValue toValue) {
        super(entity, toValue);
        this.undoEntity = undoEntity;
    }

    @Override
    protected AbstractMove<TestdataSolution> createUndoMove(ScoreDirector<TestdataSolution> scoreDirector) {
        // Corrupts the undo move by using a different entity and not undo-ing the value
        return new TestdataCorruptedEntityUndoMove(undoEntity, entity, toValue);
    }
}
