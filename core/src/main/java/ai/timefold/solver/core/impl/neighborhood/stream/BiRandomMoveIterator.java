package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * An iterator for the bi-move stream which returns (A,B) pairs in random order.
 * This iterator never ends and may return the same (A,B) pair more than once,
 * except for a left tuple whose right side has been confirmed empty
 * (no matching right tuple, or every filtered candidate was rejected up to the bail-out);
 * such a dead left tuple is retired and never picked again.
 * <p>
 * This iterator's implementation is determined by the following considerations:
 * <ol>
 * <li>The left and right datasets need to support efficient random access.</li>
 * <li>The left and right datasets are possibly large,
 * which makes their copying and mutation prohibitively expensive.</li>
 * <li>Keeping all possible pairs in memory is prohibitively expensive,
 * for the same reason.
 * (Cartesian product of A x B.)</li>
 * </ol>
 * <p>
 * From the above, the key design decisions are:
 * <ul>
 * <li>Both left and right datasets are kept in the {@link ArrayList} in which they came.
 * This list will never be copied, nor will it be mutated.</li>
 * <li>The right side of a live left tuple is drawn from a never-ending, cheap random iterator;
 * only a dead left tuple is ever removed, via {@link RetiringRandomIterator#retire()} on the left side.</li>
 * <li>Filtering of (A,B) pair only happens after both A and B have been randomly selected.
 * This guarantees that filtering is only applied when necessary,
 * as opposed to pre-filtering the entire dataset,
 * which could be prohibitively expensive.</li>
 * </ul>
 */
@NullMarked
final class BiRandomMoveIterator<Solution_, A, B>
        implements Iterator<Move<Solution_>>, RetiringBiWalk<UniTuple<A>, UniTuple<B>> {

    private final BiMoveStreamContext<Solution_, A, B> context;
    private final RandomGenerator workingRandom;

    // Fields required for iteration.
    private final RetiringRandomIterator<UniTuple<A>> leftTupleIterator;
    private @Nullable Move<Solution_> nextMove;

    public BiRandomMoveIterator(BiMoveStreamContext<Solution_, A, B> context, RandomGenerator workingRandom) {
        this.context = Objects.requireNonNull(context);
        this.workingRandom = Objects.requireNonNull(workingRandom);
        this.leftTupleIterator = context.getLeftDatasetInstance()
                .retiringRandomIterator(workingRandom);
    }

    @Override
    public boolean hasNext() {
        return nextMove != null || RetiringBiWalk.advance(leftTupleIterator, this);
    }

    @Override
    public Iterator<UniTuple<B>> createRightIterator(UniTuple<A> leftTuple) {
        var rightDatasetInstance = context.getRightDatasetInstance();
        var compositeKey = rightDatasetInstance.produceCompositeKey(leftTuple);
        var rightTupleIterator = rightDatasetInstance.randomIterator(compositeKey, workingRandom);
        var filter = rightDatasetInstance.getFilter();
        if (filter == null) { // Shortcut: no filter means we can take the plain random iterator as-is.
            return rightTupleIterator;
        }
        var solutionView = context.neighborhoodSession().getSolutionView();
        var leftFact = leftTuple.getA();
        // Random draws with replacement can never prove that no right tuple matches;
        // bail out after many consecutive rejections, same multiple as FilteringEntitySelector.
        var bailOutSize = rightDatasetInstance.size(compositeKey) * 10L;
        return new FilteringIterator<>(rightTupleIterator,
                rightTuple -> filter.test(solutionView, leftFact, rightTuple.getA()), bailOutSize);
    }

    @Override
    public void accept(UniTuple<A> leftTuple, UniTuple<B> rightTuple) {
        nextMove = context.buildMove(leftTuple.getA(), rightTuple.getA());
        if (nextMove instanceof AbstractSelectorBasedMove<Solution_> legacyMove) {
            throw new UnsupportedOperationException("""
                    Neighborhoods do not support legacy moves.
                    Please refactor your code (%s) to use the new Move API."""
                    .formatted(legacyMove.getClass().getCanonicalName()));
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

    @Override
    public void forEachRemaining(Consumer<? super Move<Solution_>> action) {
        // Effectively never ends as long as at least one (A, B) pair exists,
        // since a live left tuple is never removed.
        throw new UnsupportedOperationException("""
                This iterator does not end, so forEachRemaining() cannot terminate.
                Maybe use hasNext() and next() with your own stop condition instead.""");
    }

}
