package ai.timefold.solver.core.impl.neighborhood.selection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSelectionStrategy;
import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSource;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveIterable;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gives the moves of one step; the {@link MoveSelectionStrategy} chooses the move source of each move.
 * {@link #hasNext()} asks the strategy, {@link #next()} takes the move from the chosen move source.
 */
@NullMarked
public final class StrategyUnionMoveIterator<Solution_> implements Iterator<Move<Solution_>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyUnionMoveIterator.class);

    private final MoveSelectionStrategy<Solution_> strategy;
    private final List<DefaultMoveSource<Solution_>> moveSourceList;
    private final List<Iterator<Move<Solution_>>> moveIteratorList;
    private final boolean[] availableArray;
    private final RandomGenerator random;
    private final int stepIndex;
    private int availableCount = 0;
    private int chosenIndex = -1;
    private boolean ended = false;
    private boolean moveGiven = false;

    /**
     * @param moveIterableList the move iterable of each move source, at the same index
     */
    public StrategyUnionMoveIterator(MoveSelectionStrategy<Solution_> strategy,
            List<DefaultMoveSource<Solution_>> moveSourceList, List<MoveIterable<Solution_>> moveIterableList,
            RandomGenerator random, int stepIndex) {
        this.strategy = strategy;
        this.moveSourceList = moveSourceList;
        this.moveIteratorList = new ArrayList<>(moveSourceList.size());
        this.availableArray = new boolean[moveSourceList.size()];
        this.random = random;
        this.stepIndex = stepIndex;
        for (var i = 0; i < moveSourceList.size(); i++) {
            var moveIterator = moveIterableList.get(i).iterator(random);
            moveIteratorList.add(moveIterator);
            if (moveIterator.hasNext()) {
                availableArray[i] = true;
                availableCount++;
            } else {
                strategy.moveSourceExhausted(moveSourceList.get(i));
            }
        }
    }

    @Override
    public boolean hasNext() {
        if (chosenIndex >= 0) {
            return true;
        }
        if (ended) {
            return false;
        }
        if (availableCount == 0) {
            ended = true;
            LOGGER.debug("The step index ({}) ended because all move sources are exhausted.", stepIndex);
            return false;
        }
        var moveSource = strategy.nextMoveSource(random);
        if (moveSource == null) {
            if (!moveGiven) {
                throw new IllegalStateException("""
                        The move selection strategy (%s) gave no move source at step index (%d), \
                        but no move was selected in that step yet and (%d) move sources are still available.
                        Maybe return a move source before returning null in a step."""
                        .formatted(strategy.getClass().getSimpleName(), stepIndex, availableCount));
            }
            ended = true;
            LOGGER.debug("The step index ({}) ended because the move selection strategy ({}) gave no more move sources.",
                    stepIndex, strategy.getClass().getSimpleName());
            return false;
        }
        chosenIndex = toAvailableIndex(moveSource);
        return true;
    }

    private int toAvailableIndex(MoveSource<Solution_> moveSource) {
        if (!(moveSource instanceof DefaultMoveSource<Solution_> defaultMoveSource)
                || defaultMoveSource.index() < 0
                || defaultMoveSource.index() >= moveSourceList.size()
                || moveSourceList.get(defaultMoveSource.index()) != defaultMoveSource) {
            throw new IllegalStateException("""
                    The move selection strategy (%s) chose an unknown move source (%s).
                    Maybe return only move sources from the phase context."""
                    .formatted(strategy.getClass().getSimpleName(), moveSource));
        }
        var index = defaultMoveSource.index();
        if (!availableArray[index]) {
            throw new IllegalStateException("""
                    The move selection strategy (%s) chose the exhausted move source (%s) at step index (%d).
                    Maybe do not return a move source after moveSourceExhausted() until the next stepStarted()."""
                    .formatted(strategy.getClass().getSimpleName(), moveSource, stepIndex));
        }
        return index;
    }

    @Override
    public Move<Solution_> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        var index = chosenIndex;
        chosenIndex = -1;
        var moveIterator = moveIteratorList.get(index);
        var move = moveIterator.next();
        moveGiven = true;
        if (!moveIterator.hasNext()) {
            availableArray[index] = false;
            availableCount--;
            strategy.moveSourceExhausted(moveSourceList.get(index));
        }
        return move;
    }

}
