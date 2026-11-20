package ai.timefold.solver.core.impl.move.builtin;

import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Each draw of a left element calls {@link #tryBuildMove} up to {@link RetiringBiWalk#PROBE_ATTEMPT_COUNT} times,
 * because one failure can be a random bail-out.
 * If no call builds a move, the left element is retired.
 * Every left element with a move therefore has about the same chance, however many moves it has.
 *
 * @param <Solution_> the solution type
 * @param <Left_> the type of the left element
 */
@NullMarked
public abstract non-sealed class AbstractRepeatedDrawMoveIterator<Solution_, Left_>
        extends AbstractLeftDrivenMoveIterator<Solution_, Left_> {

    /**
     * @param leftIterator draws at random from the elements not yet retired
     */
    protected AbstractRepeatedDrawMoveIterator(RetiringRandomIterator<@Nullable Left_> leftIterator) {
        super(leftIterator);
    }

    /**
     * Each call must draw its candidates anew, so that a repeated call can succeed after a bail-out.
     *
     * @return null if no move was found for this left element
     */
    protected abstract @Nullable Move<Solution_> tryBuildMove(Left_ left);

    @Override
    final @Nullable Move<Solution_> drawMove(Left_ left) {
        for (var attempt = 0; attempt < RetiringBiWalk.PROBE_ATTEMPT_COUNT; attempt++) {
            var move = tryBuildMove(left);
            if (move != null) {
                return move;
            }
        }
        return null;
    }

    @Override
    final boolean keepsAfterFailedDraw(Left_ left) {
        return false;
    }

}
