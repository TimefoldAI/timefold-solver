package ai.timefold.solver.core.impl.neighborhood.selection;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSelectionStrategy;
import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSource;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveIterable;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gives the moves of one step; the {@link MoveSelectionStrategy} chooses the move source of each move.
 * {@link #hasNext()} asks the strategy, {@link #next()} takes the move from the chosen move source.
 * The move iterator of a move source is created only when the strategy first chooses it,
 * so that move sources the strategy does not choose cost nothing.
 */
@NullMarked
public final class StrategyUnionMoveIterator<Solution_> implements Iterator<Move<Solution_>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyUnionMoveIterator.class);

    private final MoveSelectionStrategy<Solution_> strategy;
    private final List<DefaultMoveSource<Solution_>> moveSourceList;
    private final List<MoveIterable<Solution_>> moveIterableList;
    private final @Nullable Iterator<Move<Solution_>>[] moveIteratorArray;
    private final boolean[] exhaustedArray;
    private final RandomGenerator random;
    private final int stepIndex;
    private int exhaustedCount = 0;
    private int chosenIndex = -1;
    private boolean ended = false;

    /**
     * @param moveIterableList the move iterable of each move source, at the same index
     * @param random gives the move iterators their randomness
     */
    @SuppressWarnings("unchecked")
    public StrategyUnionMoveIterator(MoveSelectionStrategy<Solution_> strategy,
            List<DefaultMoveSource<Solution_>> moveSourceList, List<MoveIterable<Solution_>> moveIterableList,
            RandomGenerator random, int stepIndex) {
        this.strategy = strategy;
        this.moveSourceList = moveSourceList;
        this.moveIterableList = moveIterableList;
        this.moveIteratorArray = new Iterator[moveSourceList.size()];
        this.exhaustedArray = new boolean[moveSourceList.size()];
        this.random = random;
        this.stepIndex = stepIndex;
    }

    @Override
    public boolean hasNext() {
        if (chosenIndex >= 0) {
            return true;
        }
        while (!ended) {
            if (exhaustedCount == moveSourceList.size()) {
                ended = true;
                LOGGER.debug("The step index ({}) ended because all move sources are exhausted.", stepIndex);
                return false;
            }
            var moveSource = strategy.nextMoveSource();
            if (moveSource == null) {
                ended = true;
                LOGGER.debug("The step index ({}) ended because the move selection strategy ({}) gave no more move sources.",
                        stepIndex, strategy.getClass().getSimpleName());
                return false;
            }
            var index = toAvailableIndex(moveSource);
            var moveIterator = moveIteratorArray[index];
            if (moveIterator == null) {
                moveIterator = moveIterableList.get(index).iterator(random);
                moveIteratorArray[index] = moveIterator;
            }
            if (moveIterator.hasNext()) {
                chosenIndex = index;
                return true;
            }
            exhaust(index);
        }
        return false;
    }

    private int toAvailableIndex(MoveSource<Solution_> moveSource) {
        if (!(moveSource instanceof DefaultMoveSource<Solution_> defaultMoveSource)
                || defaultMoveSource.index() < 0
                || defaultMoveSource.index() >= moveSourceList.size()
                || !Objects.equals(moveSourceList.get(defaultMoveSource.index()), defaultMoveSource)) {
            throw new IllegalStateException("""
                    The move selection strategy (%s) chose an unknown move source (%s).
                    Maybe return only move sources from the phase context."""
                    .formatted(strategy.getClass().getSimpleName(), moveSource));
        }
        var index = defaultMoveSource.index();
        if (exhaustedArray[index]) {
            throw new IllegalStateException("""
                    The move selection strategy (%s) chose the exhausted move source (%s) at step index (%d).
                    Maybe do not return a move source after moveSourceExhausted() until the next stepStarted()."""
                    .formatted(strategy.getClass().getSimpleName(), moveSource, stepIndex));
        }
        return index;
    }

    private void exhaust(int index) {
        exhaustedArray[index] = true;
        exhaustedCount++;
        strategy.moveSourceExhausted();
    }

    @Override
    public Move<Solution_> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        var index = chosenIndex;
        chosenIndex = -1;
        var moveIterator = Objects.requireNonNull(moveIteratorArray[index]);
        var move = moveIterator.next();
        if (!moveIterator.hasNext()) {
            exhaust(index);
        }
        return move;
    }

}
