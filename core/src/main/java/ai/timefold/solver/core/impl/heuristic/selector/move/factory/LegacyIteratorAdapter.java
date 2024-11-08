package ai.timefold.solver.core.impl.heuristic.selector.move.factory;

import java.util.Iterator;

import ai.timefold.solver.core.impl.heuristic.move.LegacyMoveAdapter;
import ai.timefold.solver.core.preview.api.move.Move;

final class LegacyIteratorAdapter<Solution_> implements Iterator<Move<Solution_>> {

    private final Iterator<ai.timefold.solver.core.impl.heuristic.move.Move<Solution_>> moveIterator;

    public LegacyIteratorAdapter(Iterator<ai.timefold.solver.core.impl.heuristic.move.Move<Solution_>> moveIterator) {
        this.moveIterator = moveIterator;
    }

    @Override
    public boolean hasNext() {
        return moveIterator.hasNext();
    }

    @Override
    public Move<Solution_> next() {
        return new LegacyMoveAdapter<>(moveIterator.next());
    }
}
