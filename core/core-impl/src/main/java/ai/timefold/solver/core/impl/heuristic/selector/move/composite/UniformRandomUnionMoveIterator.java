package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.SelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;

final class UniformRandomUnionMoveIterator<Solution_> extends SelectionIterator<Move<Solution_>> {

    private static <Solution_> List<Iterator<Move<Solution_>>> toMoveIteratorList(
            List<MoveSelector<Solution_>> childMoveSelectorList) {
        var list = new ArrayList<Iterator<Move<Solution_>>>(childMoveSelectorList.size());
        for (var moves : childMoveSelectorList) {
            var iterator = moves.iterator();
            if (iterator.hasNext()) {
                list.add(iterator);
            }
        }
        return list;
    }

    private final List<Iterator<Move<Solution_>>> moveIteratorList;
    private final Random workingRandom;

    public UniformRandomUnionMoveIterator(List<MoveSelector<Solution_>> childMoveSelectorList, Random workingRandom) {
        this.moveIteratorList = toMoveIteratorList(childMoveSelectorList);
        this.workingRandom = workingRandom;
    }

    @Override
    public boolean hasNext() {
        return !moveIteratorList.isEmpty();
    }

    @Override
    public Move<Solution_> next() {
        int index = workingRandom.nextInt(moveIteratorList.size());
        Iterator<Move<Solution_>> moveIterator = moveIteratorList.get(index);
        Move<Solution_> next = moveIterator.next();
        if (!moveIterator.hasNext()) {
            moveIteratorList.remove(index);
        }
        return next;
    }

}
