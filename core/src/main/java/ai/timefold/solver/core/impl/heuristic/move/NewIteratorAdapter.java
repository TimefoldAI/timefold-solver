package ai.timefold.solver.core.impl.heuristic.move;

import java.util.Iterator;

import ai.timefold.solver.core.preview.api.move.Move;

record NewIteratorAdapter<Solution_>(Iterator<Move<Solution_>> moveIterator)
        implements
            Iterator<ai.timefold.solver.core.impl.heuristic.move.Move<Solution_>> {

    @Override
    public boolean hasNext() {
        return moveIterator.hasNext();
    }

    @Override
    public ai.timefold.solver.core.impl.heuristic.move.Move<Solution_> next() {
        return MoveAdapters.toLegacyMove(moveIterator.next());
    }

}
