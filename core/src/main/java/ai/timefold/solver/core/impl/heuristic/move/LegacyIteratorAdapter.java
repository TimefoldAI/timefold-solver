package ai.timefold.solver.core.impl.heuristic.move;

import java.util.Iterator;

import ai.timefold.solver.core.preview.api.move.Move;

record LegacyIteratorAdapter<Solution_>(Iterator<ai.timefold.solver.core.impl.heuristic.move.Move<Solution_>> moveIterator)
        implements
            Iterator<Move<Solution_>> {

    @Override
    public boolean hasNext() {
        return moveIterator.hasNext();
    }

    @Override
    public Move<Solution_> next() {
        return MoveAdapters.toNewMove(moveIterator.next());
    }

}
