package ai.timefold.solver.core.impl.move.builtin;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Each draw of a left element calls {@link #tryBuildMove} once,
 * which makes one draw from a pool that is the same for every left element.
 * A left element with more moves therefore succeeds more often.
 * After a failure, this iterator draws another left element instead of retrying the same one,
 * so every move has about the same chance, however many moves its left element has.
 * A retry of the same left element would give every left element the same chance instead.
 * <p>
 * A failed single draw does not show that the left element has no move.
 * Before it retires the element, this iterator calls {@link #hasMove}
 * up to {@link RetiringBiWalk#PROBE_ATTEMPT_COUNT} times, because one failure can be a random bail-out.
 * A left element known to have a move is remembered until the iterator ends,
 * so {@link #hasMove} runs at most a few times per left element.
 *
 * @param <Solution_> the solution type
 * @param <Left_> the type of the left element
 */
@NullMarked
public abstract non-sealed class AbstractSingleDrawMoveIterator<Solution_, Left_>
        extends AbstractLeftDrivenMoveIterator<Solution_, Left_> {

    private final Set<Left_> leftsWithMove = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * @param leftIterator draws at random from the elements not yet retired
     */
    protected AbstractSingleDrawMoveIterator(RetiringRandomIterator<@Nullable Left_> leftIterator) {
        super(leftIterator);
    }

    /**
     * Makes one draw from a pool that is the same for every left element.
     *
     * @return null if the draw gave no valid move
     */
    protected abstract @Nullable Move<Solution_> tryBuildMove(Left_ left);

    /**
     * Searches for a move of the left element, with as many draws as needed.
     * A move it finds is not used: using it would give the left element an extra chance.
     *
     * @return true if a move was found for this left element
     */
    protected abstract boolean hasMove(Left_ left);

    @Override
    final @Nullable Move<Solution_> drawMove(Left_ left) {
        return tryBuildMove(left);
    }

    @Override
    final boolean keepsAfterFailedDraw(Left_ left) {
        if (leftsWithMove.contains(left)) {
            return true;
        }
        for (var attempt = 0; attempt < RetiringBiWalk.PROBE_ATTEMPT_COUNT; attempt++) {
            if (hasMove(left)) {
                leftsWithMove.add(left);
                return true;
            }
        }
        return false;
    }

}
