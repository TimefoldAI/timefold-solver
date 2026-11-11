package ai.timefold.solver.core.impl.move.builtin;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.FilteringIterator;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws a left element at random from the elements not yet retired, and builds a move with it.
 * An element that gives a move stays in the pool and can be drawn again.
 * Subclasses draw the partner from a narrowed pool where possible,
 * such as {@link SolutionView#getEntitiesWithValueInRange}, and still check each candidate.
 * <p>
 * If a draw gives no move, the subclass decides whether the element stays in the pool or is retired.
 * A retired element is never drawn again; a null left element is retired at once.
 * The iterator ends once every left element is retired.
 * The subclasses decide differently, and so give different moves more often:
 * <ul>
 * <li>{@link AbstractRepeatedDrawMoveIterator}: every left element with a move has about the same chance.</li>
 * <li>{@link AbstractSingleDrawMoveIterator}: every move has about the same chance.</li>
 * </ul>
 *
 * @param <Solution_> the solution type
 * @param <Left_> the type of the left element
 */
@NullMarked
public abstract sealed class AbstractLeftDrivenMoveIterator<Solution_, Left_> implements Iterator<Move<Solution_>>
        permits AbstractRepeatedDrawMoveIterator, AbstractSingleDrawMoveIterator {

    private final RetiringRandomIterator<@Nullable Left_> leftIterator;
    private @Nullable Move<Solution_> nextMove;

    /**
     * @param leftIterator draws at random from the elements not yet retired
     */
    AbstractLeftDrivenMoveIterator(RetiringRandomIterator<@Nullable Left_> leftIterator) {
        this.leftIterator = Objects.requireNonNull(leftIterator);
    }

    /**
     * @return null if this draw of the left element gave no move
     */
    abstract @Nullable Move<Solution_> drawMove(Left_ left);

    /**
     * Called after {@link #drawMove} gave no move.
     *
     * @return true to keep the left element in the pool, false to retire it
     */
    abstract boolean keepsAfterFailedDraw(Left_ left);

    @Override
    public final boolean hasNext() {
        if (nextMove != null) {
            return true;
        }
        while (leftIterator.hasNext()) {
            var left = leftIterator.next();
            if (left != null) {
                nextMove = drawMove(left);
                if (nextMove != null) {
                    return true;
                }
                if (keepsAfterFailedDraw(left)) {
                    continue;
                }
            }
            leftIterator.retire();
        }
        return false;
    }

    @Override
    public final Move<Solution_> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        var move = nextMove;
        nextMove = null;
        return move;
    }

    /**
     * @return {@code candidateCount * BAIL_OUT_SAFETY_MULTIPLIER}, or {@link Long#MAX_VALUE} if that overflows
     */
    protected static long bailOutBudget(long candidateCount) {
        return candidateCount > Long.MAX_VALUE / FilteringIterator.BAIL_OUT_SAFETY_MULTIPLIER ? Long.MAX_VALUE
                : candidateCount * FilteringIterator.BAIL_OUT_SAFETY_MULTIPLIER;
    }

    /**
     * Draws with replacement until the filter accepts,
     * at most {@code candidateList.size() * BAIL_OUT_SAFETY_MULTIPLIER} times.
     *
     * @param candidateList random access, no null elements
     * @return null if nothing was accepted
     */
    protected static <T> @Nullable T drawMatching(List<T> candidateList, RandomGenerator random, Predicate<T> filter) {
        var size = candidateList.size();
        if (size == 0) {
            return null;
        }
        var budget = bailOutBudget(size);
        for (var attempt = 0L; attempt < budget; attempt++) {
            var candidate = candidateList.get(random.nextInt(size));
            if (filter.test(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * As {@link #drawMatching(List, RandomGenerator, Predicate)}, but from a never-ending random iterator.
     *
     * @param randomIterator may return null only if the filter rejects it
     * @param candidateCount the size of the population behind the iterator
     */
    protected static <T> @Nullable T drawMatching(Iterator<T> randomIterator, long candidateCount, Predicate<T> filter) {
        var budget = bailOutBudget(candidateCount);
        for (var attempt = 0L; attempt < budget && randomIterator.hasNext(); attempt++) {
            var candidate = randomIterator.next();
            if (filter.test(candidate)) {
                return candidate;
            }
        }
        return null;
    }

}
