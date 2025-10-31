package ai.timefold.solver.core.impl.neighborhood.move;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniDatasetInstance;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * An iterator for the bi-move stream which enumerates all pairs in the given order.
 * It goes through all tuples coming from the left, and for each left tuple,
 * it goes through all tuples coming from the right that match the joiner and filter.
 * It is more efficient than {@link BiRandomMoveIterator},
 * because it does not need to enumerate any list of tuples up-front
 * and all access happens on-demand.
 */
@NullMarked
final class BiOriginalMoveIterator<Solution_, A, B> implements Iterator<Move<Solution_>> {

    private static final UniTuple EMPTY_TUPLE = new UniTuple<>(null, 0);

    private final BiMoveStreamContext<Solution_, A, B> context;
    private final UniDatasetInstance<Solution_, A> leftDatasetInstance;
    private final UniDatasetInstance<Solution_, B> rightDatasetInstance;

    // Fields required for iteration.
    private @Nullable Move<Solution_> nextMove;
    private @Nullable Iterator<UniTuple<A>> leftTupleIterator;
    private @Nullable Iterator<UniTuple<B>> rightTupleIterator;
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
        var joiner = context.getJoiner();
        var filter = context.getFilter();
        var solutionView = context.neighborhoodSession().getSolutionView();
        while (true) {
            if (rightTupleIterator == null || !rightTupleIterator.hasNext()) {
                if (leftTupleIterator.hasNext()) { // The second iterator is exhausted or the first one was not yet created.
                    leftTuple = leftTupleIterator.next();
                    rightTupleIterator =
                            new JoiningIterator<>(joiner, filter, solutionView, leftTuple, rightDatasetInstance.iterator());
                } else { // No more elements in both iterators.
                    return false;
                }
            } else { // Both iterators have elements.
                var leftFact = leftTuple.factA;
                var rightFact = rightTupleIterator.next().factA;
                nextMove = context.buildMove(leftFact, rightFact);
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
