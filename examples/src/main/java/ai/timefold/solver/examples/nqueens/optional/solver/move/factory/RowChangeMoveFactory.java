package ai.timefold.solver.examples.nqueens.optional.solver.move.factory;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveListFactory;
import ai.timefold.solver.examples.nqueens.domain.NQueens;
import ai.timefold.solver.examples.nqueens.domain.Queen;
import ai.timefold.solver.examples.nqueens.domain.Row;
import ai.timefold.solver.examples.nqueens.optional.solver.move.RowChangeMove;

public class RowChangeMoveFactory implements MoveListFactory<NQueens> {

    @Override
    public List<RowChangeMove> createMoveList(NQueens nQueens) {
        List<RowChangeMove> moveList = new ArrayList<>();
        for (Queen queen : nQueens.getQueenList()) {
            for (Row toRow : nQueens.getRowList()) {
                moveList.add(new RowChangeMove(queen, toRow));
            }
        }
        return moveList;
    }

}
