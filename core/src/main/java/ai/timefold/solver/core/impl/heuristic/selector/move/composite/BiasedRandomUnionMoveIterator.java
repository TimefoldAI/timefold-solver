package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.SelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.solver.random.BiasedRandomChooser;
import ai.timefold.solver.core.preview.api.move.Move;

final class BiasedRandomUnionMoveIterator<Solution_> extends SelectionIterator<Move<Solution_>> {

    private final BiasedRandomChooser<Iterator<Move<Solution_>>> chooser;
    private final RandomGenerator workingRandom;

    public BiasedRandomUnionMoveIterator(List<MoveSelector<Solution_>> childMoveSelectorList,
            ToDoubleFunction<MoveSelector<Solution_>> probabilityWeightFunction,
            RandomGenerator workingRandom) {
        var moveIteratorList = new ArrayList<Iterator<Move<Solution_>>>(childMoveSelectorList.size());
        var probabilityWeightArray = new double[childMoveSelectorList.size()];
        for (var i = 0; i < childMoveSelectorList.size(); i++) {
            var moveSelector = childMoveSelectorList.get(i);
            moveIteratorList.add(moveSelector.iterator());
            probabilityWeightArray[i] = probabilityWeightFunction.applyAsDouble(moveSelector);
        }
        this.chooser = new BiasedRandomChooser<>(moveIteratorList, probabilityWeightArray, Iterator::hasNext);
        this.workingRandom = workingRandom;
    }

    @Override
    public boolean hasNext() {
        return !chooser.isEmpty();
    }

    @Override
    public Move<Solution_> next() {
        var moveIterator = chooser.choose(workingRandom);
        if (moveIterator == null) {
            throw new NoSuchElementException();
        }
        var move = moveIterator.next();
        if (!moveIterator.hasNext()) {
            chooser.markStale();
        }
        return move;
    }

}
