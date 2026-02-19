package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import ai.timefold.solver.core.impl.heuristic.move.SelectorBasedCompositeMove;
import ai.timefold.solver.core.impl.heuristic.move.SelectorBasedNoChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.SelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.preview.api.move.Move;

/**
 * A {@link CompositeMoveSelector} that Cartesian products 2 or more {@link MoveSelector}s.
 * <p>
 * For example: a Cartesian product of {A, B, C} and {X, Y} will result in {AX, AY, BX, BY, CX, CY}.
 * <p>
 * Warning: there is no duplicated {@link Move} check, so union of {A, B} and {B} will result in {AB, BB}.
 *
 * @see CompositeMoveSelector
 */
public class CartesianProductMoveSelector<Solution_> extends CompositeMoveSelector<Solution_> {

    private static final Move<?> EMPTY_MARK = SelectorBasedNoChangeMove.getInstance();

    private final boolean ignoreEmptyChildIterators;

    public CartesianProductMoveSelector(List<MoveSelector<Solution_>> childMoveSelectorList, boolean ignoreEmptyChildIterators,
            boolean randomSelection) {
        super(childMoveSelectorList, randomSelection);
        this.ignoreEmptyChildIterators = ignoreEmptyChildIterators;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isNeverEnding() {
        if (randomSelection) {
            return true;
        } else {
            // Only the last childMoveSelector can be neverEnding
            return !childMoveSelectorList.isEmpty()
                    && childMoveSelectorList.get(childMoveSelectorList.size() - 1).isNeverEnding();
        }
    }

    @Override
    public long getSize() {
        var size = 0L;
        for (var moveSelector : childMoveSelectorList) {
            var childSize = moveSelector.getSize();
            if (childSize == 0L) {
                if (!ignoreEmptyChildIterators) {
                    return 0L;
                }
                // else ignore that child
            } else {
                if (size == 0L) {
                    // There must be at least 1 non-empty child to change the size from 0
                    size = childSize;
                } else {
                    size *= childSize;
                }
            }
        }
        return size;
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        if (!randomSelection) {
            return new OriginalCartesianProductMoveIterator();
        } else {
            return new RandomCartesianProductMoveIterator();
        }
    }

    public class OriginalCartesianProductMoveIterator extends UpcomingSelectionIterator<Move<Solution_>> {

        private final List<Iterator<Move<Solution_>>> moveIteratorList;

        private Move<Solution_>[] subSelections;

        public OriginalCartesianProductMoveIterator() {
            moveIteratorList = new ArrayList<>(childMoveSelectorList.size());
            for (var i = 0; i < childMoveSelectorList.size(); i++) {
                moveIteratorList.add(null);
            }
            subSelections = null;
        }

        @Override
        protected Move<Solution_> createUpcomingSelection() {
            var childSize = moveIteratorList.size();
            int startingIndex;
            var moveList = new Move[childSize];
            if (subSelections == null) {
                startingIndex = -1;
            } else {
                startingIndex = findStartingIndex(childSize);
                if (startingIndex < 0) {
                    return noUpcomingSelection();
                }
                // Clone to avoid CompositeMove corruption
                System.arraycopy(subSelections, 0, moveList, 0, startingIndex);
                moveList[startingIndex] = moveIteratorList.get(startingIndex).next();
            }
            for (var i = startingIndex + 1; i < childSize; i++) {
                var moveIterator = childMoveSelectorList.get(i).iterator();
                moveIteratorList.set(i, moveIterator);
                if (!moveIterator.hasNext()) { // in case a moveIterator is empty
                    if (ignoreEmptyChildIterators) {
                        moveList[i] = EMPTY_MARK;
                    } else {
                        return noUpcomingSelection();
                    }
                } else {
                    moveList[i] = moveIterator.next();
                }
            }
            // No need to clone to avoid CompositeMove corruption because subSelections's elements never change
            subSelections = moveList;
            return buildMove(moveList, childSize);
        }

        private int findStartingIndex(int childSize) {
            var startingIndex = childSize - 1;
            while (startingIndex >= 0) {
                var moveIterator = moveIteratorList.get(startingIndex);
                if (moveIterator.hasNext()) {
                    break;
                }
                startingIndex--;
            }
            return startingIndex;
        }

        private Move<Solution_> buildMove(Move<Solution_>[] moveList, int childSize) {
            if (!ignoreEmptyChildIterators) {
                return SelectorBasedCompositeMove.buildMove(moveList);
            }
            // Clone because EMPTY_MARK should survive in subSelections
            Move<Solution_>[] newMoveList = new Move[childSize];
            var newSize = 0;
            for (var i = 0; i < childSize; i++) {
                if (moveList[i] != EMPTY_MARK) {
                    newMoveList[newSize] = moveList[i];
                    newSize++;
                }
            }
            return switch (newSize) {
                case 0 -> noUpcomingSelection();
                case 1 -> newMoveList[0];
                default -> SelectorBasedCompositeMove.buildMove(Arrays.copyOfRange(newMoveList, 0, newSize));
            };
        }

    }

    public class RandomCartesianProductMoveIterator extends SelectionIterator<Move<Solution_>> {

        private final List<Iterator<Move<Solution_>>> moveIteratorList;
        private Boolean empty;

        public RandomCartesianProductMoveIterator() {
            moveIteratorList = new ArrayList<>(childMoveSelectorList.size());
            empty = null;
            for (var moveSelector : childMoveSelectorList) {
                moveIteratorList.add(moveSelector.iterator());
            }
        }

        @Override
        public boolean hasNext() {
            if (empty == null) { // Only done in the first call
                var emptyCount = 0;
                for (var moveIterator : moveIteratorList) {
                    if (!moveIterator.hasNext()) {
                        emptyCount++;
                        if (!ignoreEmptyChildIterators) {
                            break;
                        }
                    }
                }
                empty = ignoreEmptyChildIterators ? emptyCount == moveIteratorList.size() : emptyCount > 0;
            }
            return !empty;
        }

        @Override
        public Move<Solution_> next() {
            List<Move<Solution_>> moveList = new ArrayList<>(moveIteratorList.size());
            for (var i = 0; i < moveIteratorList.size(); i++) {
                var moveIterator = moveIteratorList.get(i);
                var skip = false;
                if (!moveIterator.hasNext()) {
                    var moveSelector = childMoveSelectorList.get(i);
                    moveIterator = moveSelector.iterator();
                    moveIteratorList.set(i, moveIterator);
                    if (!moveIterator.hasNext()) {
                        if (ignoreEmptyChildIterators) {
                            skip = true;
                        } else {
                            throw new NoSuchElementException(
                                    "The iterator of childMoveSelector (" + moveSelector + ") is empty.");
                        }
                    }
                }
                if (!skip) {
                    moveList.add(moveIterator.next());
                }
            }
            if (ignoreEmptyChildIterators) {
                if (moveList.isEmpty()) {
                    throw new NoSuchElementException(
                            "All iterators of childMoveSelectorList (" + childMoveSelectorList + ") are empty.");
                } else if (moveList.size() == 1) {
                    return moveList.get(0);
                }
            }
            return SelectorBasedCompositeMove.buildMove(moveList.toArray(new Move[0]));
        }

    }

    @Override
    public String toString() {
        return "CartesianProduct(" + childMoveSelectorList + ")";
    }

}
