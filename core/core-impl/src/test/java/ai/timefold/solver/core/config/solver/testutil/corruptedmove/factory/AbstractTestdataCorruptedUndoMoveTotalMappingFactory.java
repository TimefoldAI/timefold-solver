package ai.timefold.solver.core.config.solver.testutil.corruptedmove.factory;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.config.solver.testutil.corruptedmove.AbstractTestdataMove;
import ai.timefold.solver.core.config.solver.testutil.corruptedmove.TestdataCorruptedEntityUndoMove;
import ai.timefold.solver.core.config.solver.testutil.corruptedmove.TestdataCorruptedUndoMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveListFactory;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

public class AbstractTestdataCorruptedUndoMoveTotalMappingFactory implements MoveListFactory<TestdataSolution> {

    private boolean corruptEntityAsWell;

    AbstractTestdataCorruptedUndoMoveTotalMappingFactory(boolean corruptEntityAsWell) {
        this.corruptEntityAsWell = corruptEntityAsWell;
    }

    @Override
    public List<? extends Move<TestdataSolution>> createMoveList(TestdataSolution solution) {
        List<AbstractTestdataMove> moveList = new ArrayList<>();

        for (TestdataEntity entity : solution.getEntityList()) {
            for (TestdataValue value : solution.getValueList()) {
                if (corruptEntityAsWell) {
                    for (TestdataEntity undoEntity : solution.getEntityList()) {
                        if (entity == undoEntity) {
                            continue;
                        }
                        moveList.add(new TestdataCorruptedEntityUndoMove(entity, undoEntity, value));
                    }
                } else {
                    moveList.add(new TestdataCorruptedUndoMove(entity, value));
                }
            }
        }
        return moveList;
    }
}
