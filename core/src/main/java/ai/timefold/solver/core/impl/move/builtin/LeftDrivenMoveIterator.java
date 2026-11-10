package ai.timefold.solver.core.impl.move.builtin;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.neighborhood.stream.FilteringIterator;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws a left element with replacement, then tries to build a move for it from candidates
 * that are already known to be valid, such as those from
 * {@link SolutionView#getEntitiesWithValueInRange}.
 * Draws can repeat; no left element is ever retired, because retiring is expensive.
 * <p>
 * The iterator ends when the left side is empty,
 * or after {@code leftCount * }{@link FilteringIterator#BAIL_OUT_SAFETY_MULTIPLIER} left draws in a row produce no move.
 * The bound scales with the left side, so that left elements without any valid partner
 * do not end the iteration early while some other left element still has one.
 *
 * @param <Solution_> the solution type
 * @param <Left_> the type of the left element
 */
@NullMarked
public abstract class LeftDrivenMoveIterator<Solution_, Left_> implements Iterator<Move<Solution_>> {

    private final Iterator<? extends @Nullable Left_> leftIterator;
    private final long maxFailedDraws;
    private @Nullable Move<Solution_> nextMove;

    /**
     * @param leftIterator draws with replacement
     * @param leftCount the size of the population behind the left iterator
     */
    protected LeftDrivenMoveIterator(Iterator<? extends @Nullable Left_> leftIterator, long leftCount) {
        this.leftIterator = Objects.requireNonNull(leftIterator);
        this.maxFailedDraws = bailOutBudget(Math.max(1L, leftCount));
    }

    /**
     * @return null if no move was found for this left element
     */
    protected abstract @Nullable Move<Solution_> tryBuildMove(Left_ left);

    @Override
    public final boolean hasNext() {
        if (nextMove != null) {
            return true;
        }
        for (var failedDraws = 0L; failedDraws < maxFailedDraws && leftIterator.hasNext(); failedDraws++) {
            var left = leftIterator.next();
            if (left == null) {
                continue;
            }
            nextMove = tryBuildMove(left);
            if (nextMove != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final Move<Solution_> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        var move = Objects.requireNonNull(nextMove);
        nextMove = null;
        return move;
    }

    /**
     * @return {@code candidateCount * BAIL_OUT_SAFETY_MULTIPLIER}, or {@link Long#MAX_VALUE} if that overflows
     */
    public static long bailOutBudget(long candidateCount) {
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
    public static <T> @Nullable T drawMatching(List<T> candidateList, RandomGenerator random, Predicate<T> filter) {
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
    public static <T> @Nullable T drawMatching(Iterator<T> randomIterator, long candidateCount, Predicate<T> filter) {
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
