package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniLeftDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniRightDatasetInstance;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Original iterators build moves based on all applicable pairs of A and B instances,
 * in the order in which they appear.
 * This is a simple behavior that just enumerates all possible combinations.
 * First an instance of A is fixed, and then all instances of B are iterated.
 * Then the next instance of A is fixed, and all instances of B are iterated again.
 * This continues until all instances of A have been fixed.
 */
@NullMarked
final class BiOriginalMoveIterator<Solution_, A, B> implements Iterator<Move<Solution_>> {

    @SuppressWarnings("rawtypes")
    private static final UniTuple EMPTY_TUPLE = UniTuple.of(0);

    private final BiMoveStreamContext<Solution_, A, B> context;
    private final UniLeftDatasetInstance<Solution_, A> leftDatasetInstance;
    private final UniRightDatasetInstance<Solution_, A, B> rightDatasetInstance;

    // Fields required for iteration.
    private @Nullable Move<Solution_> nextMove;
    private @Nullable Iterator<UniTuple<A>> leftTupleIterator;
    private @Nullable Iterator<UniTuple<B>> rightTupleIterator;

    @SuppressWarnings("unchecked")
    private UniTuple<A> leftTuple = EMPTY_TUPLE;

    public BiOriginalMoveIterator(BiMoveStreamContext<Solution_, A, B> context) {
        this.context = Objects.requireNonNull(context);
        this.leftDatasetInstance = context.getLeftDatasetInstance();
        this.rightDatasetInstance = context.getRightDatasetInstance();
    }

    @Override
    public boolean hasNext() {
        // If we already found the next move, return true.
        if (nextMove != null) {
            return true;
        }

        // Initialize if needed.
        if (leftTupleIterator == null) {
            leftTupleIterator = leftDatasetInstance.iterator();
            // If first iterator is empty, there's no next move.
            if (!leftTupleIterator.hasNext()) {
                return false;
            }
        }

        // Try to find the next valid move.
        while (true) {
            if (rightTupleIterator == null || !rightTupleIterator.hasNext()) {
                if (leftTupleIterator.hasNext()) { // The second iterator is exhausted or the first one was not yet created.
                    leftTuple = leftTupleIterator.next();
                    var filter = rightDatasetInstance.getFilter();
                    rightTupleIterator = rightDatasetInstance.iterator(rightDatasetInstance.produceCompositeKey(leftTuple));
                    if (filter != null) {
                        rightTupleIterator = new FilteringIterator<>(filter, context.neighborhoodSession().getSolutionView(),
                                leftTuple, rightTupleIterator);
                    }
                } else { // No more elements in both iterators.
                    return false;
                }
            } else { // Both iterators have elements.
                var leftFact = leftTuple.getA();
                var rightFact = rightTupleIterator.next().getA();
                nextMove = context.buildMove(leftFact, rightFact);
                if (nextMove instanceof ai.timefold.solver.core.impl.heuristic.move.Move<Solution_> legacyMove) {
                    throw new UnsupportedOperationException("""
                            Neighborhoods do not support legacy moves.
                            Please refactor your code (%s) to use the new Move API."""
                            .formatted(legacyMove.getClass().getCanonicalName()));
                }
                return true;
            }
        }
    }

    @Override
    public Move<Solution_> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        var result = Objects.requireNonNull(nextMove);
        nextMove = null;
        return result;
    }

}
