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
 * After a failure, {@link #keepsAfterFailedDraw} keeps the element in the pool
 * and the loop in {@link AbstractLeftDrivenMoveIterator#hasNext()} draws the next left element;
 * it does not retry the same one.
 * So every move has about the same chance, however many moves its left element has.
 * For example, with a pool of 10 candidates, a left element A with 4 valid candidates and B with 1:
 * A gives a move in 4 of 10 draws and B in 1 of 10, so each of the 5 moves has the same chance.
 * A retry of the same left element until success would give A and B the same chance instead,
 * and each move of B would be 4 times as likely as each move of A.
 * <p>
 * A failed single draw does not show that the left element has no move.
 * Before it retires the element, this iterator calls {@link #hasMove}
 * up to {@link RetiringBiWalk#PROBE_ATTEMPT_COUNT} times, because one failure can be a random bail-out.
 * A left element known to have a move is remembered until the iterator ends,
 * so {@link #hasMove} runs at most a few times per left element.
 * A draw that gives a move is remembered only after the first failed draw;
 * when moves are common, this skips the cost, and when they are rare, the first draws fail.
 *
 * @param <Solution_> the solution type
 * @param <Left_> the type of the left element
 */
@NullMarked
public abstract non-sealed class AbstractSingleDrawMoveIterator<Solution_, Left_>
        extends AbstractLeftDrivenMoveIterator<Solution_, Left_> {

    private final Set<Left_> leftsWithMove = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean failedDrawSeen = false;

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
        var move = tryBuildMove(left);
        if (move != null && failedDrawSeen) {
            leftsWithMove.add(left);
        }
        return move;
    }

    // True keeps the element and makes the loop in AbstractLeftDrivenMoveIterator.hasNext() draw the next one.
    @Override
    final boolean keepsAfterFailedDraw(Left_ left) {
        failedDrawSeen = true;
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
