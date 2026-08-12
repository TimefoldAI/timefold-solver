package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.Iterator;

import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;

import org.jspecify.annotations.NullMarked;

/**
 * Shared walk over (left, right) pairs, drawn with replacement,
 * where a left value is only retired once {@link #PROBE_ATTEMPT_COUNT}
 * independent probes of its right side all come back empty, not just one;
 * see {@link #PROBE_ATTEMPT_COUNT}'s javadoc for why one probe is not proof.
 * Implemented as a static method plus a callback interface rather than a base class,
 * so the caller's own fields hold the pending pair
 * and no allocation is needed per {@link #advance(RetiringRandomIterator, RetiringBiWalk)} call.
 *
 * @param <L> the left value type
 * @param <R> the right value type
 */
@NullMarked
public interface RetiringBiWalk<L, R> {

    /**
     * How many independent, fresh right-iterator probes {@link #advance} gives a left before retiring it.
     * A {@link FilteringIterator} bail-out (see its javadoc) is a per-{@code hasNext()}-call false negative,
     * not proof of emptiness:
     * {@code FilteringIterator.BAIL_OUT_SAFETY_MULTIPLIER} already bounds any single probe's false-negative rate
     * at about {@code e^-BAIL_OUT_SAFETY_MULTIPLIER},
     * so 3 independent probes compound that to about {@code e^-(3 * BAIL_OUT_SAFETY_MULTIPLIER)}.
     * This does not eliminate false retirement entirely, but bounds it significantly.
     */
    int PROBE_ATTEMPT_COUNT = 3;

    /**
     * Builds the (possibly filtered, possibly bailing-out) iterator
     * over the right values matching the given left value.
     */
    Iterator<R> createRightIterator(L left);

    /**
     * Called once a matching (left, right) pair has been found;
     * stores it as the pending pair or builds the resulting move,
     * depending on the caller.
     */
    void accept(L left, R right);

    /**
     * Called right before a left value is retired for having no matching right value.
     * No-op by default;
     * only a caller which caches per-left state (such as a right iterator) needs to prune it here.
     */
    default void onExhausted(L left) {
        // Nothing to do by default.
    }

    /**
     * Whether a left value just drawn by {@link #advance} should be used for this draw,
     * or skipped and redrawn.
     * True by default:
     * every left is equally acceptable, unless the caller weights them.
     * A rejected left is skipped, not retired: {@link #advance} simply draws again.
     */
    default boolean acceptLeft(L left) {
        return true;
    }

    /**
     * Draws left values until one is accepted (see {@link #acceptLeft}) and has a matching right value,
     * retiring every dead left value along the way,
     * or until every left value is exhausted.
     * A left is only retired after {@link #PROBE_ATTEMPT_COUNT} independent, freshly built right iterators
     * in a row report no next value, not after just one:
     * {@link #createRightIterator} is called again from scratch on every attempt,
     * so each gets its own fresh bail-out budget.
     *
     * @return true if a pair was found and passed to {@link #accept(Object, Object)}
     */
    static <L, R> boolean advance(RetiringRandomIterator<L> leftIterator, RetiringBiWalk<L, R> walk) {
        while (leftIterator.hasNext()) {
            var left = leftIterator.next();
            if (!walk.acceptLeft(left)) {
                continue;
            }
            for (var attempt = 0; attempt < PROBE_ATTEMPT_COUNT; attempt++) {
                var rightIterator = walk.createRightIterator(left);
                if (rightIterator.hasNext()) {
                    walk.accept(left, rightIterator.next());
                    return true;
                }
            }
            walk.onExhausted(left);
            leftIterator.retire();
        }
        return false;
    }

}
