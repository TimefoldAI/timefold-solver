package ai.timefold.solver.core.impl.move.builtin;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.FilteringIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws a left element at random from the elements not yet retired;
 * an element that gives a move stays in the pool and can be drawn again.
 * Subclasses draw the partner from a narrowed pool where possible,
 * such as {@link SolutionView#getEntitiesWithValueInRange}, and still check each candidate.
 * <p>
 * Each draw calls {@link #tryBuildMove} up to {@link RetiringBiWalk#PROBE_ATTEMPT_COUNT} times,
 * because one failure can be a random bail-out.
 * If no call builds a move, this iterator retires the element and never draws it again;
 * a null left element is retired at once.
 * The iterator ends once every left element is retired.
 *
 * @param <Solution_> the solution type
 * @param <Left_> the type of the left element
 */
@NullMarked
public abstract class AbstractLeftDrivenMoveIterator<Solution_, Left_> implements Iterator<Move<Solution_>> {

    private final RetiringRandomIterator<@Nullable Left_> leftIterator;
    private @Nullable Move<Solution_> nextMove;

    /**
     * @param leftIterator draws at random from the elements not yet retired
     */
    protected AbstractLeftDrivenMoveIterator(RetiringRandomIterator<@Nullable Left_> leftIterator) {
        this.leftIterator = Objects.requireNonNull(leftIterator);
    }

    /**
     * Each call must draw its candidates anew, so that a repeated call can succeed after a bail-out.
     *
     * @return null if no move was found for this left element
     */
    protected abstract @Nullable Move<Solution_> tryBuildMove(Left_ left);

    @Override
    public final boolean hasNext() {
        if (nextMove != null) {
            return true;
        }
        while (leftIterator.hasNext()) {
            var left = leftIterator.next();
            if (left != null) {
                // A failed attempt can be a bail-out; only several in a row show that the left has no move.
                for (var attempt = 0; attempt < RetiringBiWalk.PROBE_ATTEMPT_COUNT; attempt++) {
                    nextMove = tryBuildMove(left);
                    if (nextMove != null) {
                        return true;
                    }
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
