package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.Iterator;

import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;

import org.jspecify.annotations.NullMarked;

/**
 * Shared walk over (left, right) pairs, drawn with replacement,
 * where a left value is only ever retired once its right side is confirmed empty.
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
     * Draws left values until one has a matching right value,
     * retiring every dead left value along the way,
     * or until every left value is exhausted.
     *
     * @return true if a pair was found and passed to {@link #accept(Object, Object)}
     */
    static <L, R> boolean advance(RetiringRandomIterator<L> leftIterator, RetiringBiWalk<L, R> walk) {
        while (leftIterator.hasNext()) {
            var left = leftIterator.next();
            var rightIterator = walk.createRightIterator(left);
            if (rightIterator.hasNext()) {
                walk.accept(left, rightIterator.next());
                return true;
            }
            walk.onExhausted(left);
            leftIterator.retire();
        }
        return false;
    }

}
