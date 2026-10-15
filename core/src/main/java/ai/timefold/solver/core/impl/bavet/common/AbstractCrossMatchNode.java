package ai.timefold.solver.core.impl.bavet.common;

import java.util.function.Consumer;

import ai.timefold.solver.core.impl.bavet.common.tuple.InTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleList;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.jspecify.annotations.NullMarked;

/**
 * The two-input nodes that cross-match a left tuple against a right tuple through a user predicate:
 * {@link AbstractJoinNode} and {@link AbstractIfExistsNode}.
 * This class owns everything they share about <i>when</i> that cross-match runs;
 * what the cross-match then does with a matching pair
 * (materialize an out-tuple, or bump an {@link ExistsCounter})
 * is entirely the subclass's business and deliberately not shared.
 * <p>
 * A filtering node whose two inputs are far enough apart (see {@link #canDeferWork()})
 * defers its cross-match computation (the opposite-side walk)
 * from "whenever a parent propagates in" to this node's own layer turn (see {@link #prepareForSettle()}),
 * closing the stale-activity race at its root instead of guarding against it per read.
 * The others read the opposite side on the spot,
 * where the per-read {@code isActive()} guards are enough.
 * A non-filtering node never dereferences a fact through a user predicate,
 * so a stale-but-"active" read can't corrupt anything there (confirmed by dedicated regression tests);
 * for those the pending fields stay {@code null}/{@code -1} and cost nothing.
 * <p>
 * {@link #pendingLeft}/{@link #pendingRight} hold tuples whose cross-match is due;
 * the marker slots exist purely to make enqueueing idempotent
 * (a tuple already awaiting its turn isn't re-added).
 * Both lists are drained in {@link #prepareForSettle()},
 * calling {@link #reconcilePendingLeft(Tuple)}/{@link #reconcilePendingRight(UniTuple)}.
 * That logic already treats "nothing recorded yet for this pair" as "record it if the predicate passes",
 * so it doubles as the insert path too.
 * <p>
 * The two sides are never both examined at once, and the drain order differs per subclass;
 * see {@code drainRightFirst} on the constructor.
 *
 * @param <LeftTuple_>
 * @param <Right_>
 */
@NullMarked
public abstract class AbstractCrossMatchNode<LeftTuple_ extends Tuple, Right_>
        extends AbstractTwoInputNode<LeftTuple_, UniTuple<Right_>>
        implements DeferredSettleAware {

    protected final boolean isFiltering;

    /**
     * Whether {@link #prepareForSettle()} drains {@link #pendingRight} before {@link #pendingLeft}.
     * See the constructor's own parameter documentation for why each subclass picks what it picks.
     */
    private final boolean drainRightFirst;

    private final int pendingLeftMarkerIndex;
    private final int pendingRightMarkerIndex;
    private final TupleList<LeftTuple_> pendingLeft;
    private final TupleList<UniTuple<Right_>> pendingRight;
    // Exists to prevent creating capturing lambdas on the hot path.
    private final Consumer<LeftTuple_> pendingLeftConsumer;
    private final Consumer<UniTuple<Right_>> pendingRightConsumer;

    private long settleDistance = -1;
    private boolean preloadingActive;
    /**
     * Whether {@link #crossMatchLeft}/{@link #crossMatchRight} actually defer,
     * as opposed to reading the opposite side on the spot;
     * precomputed because their callers sit on hot insert and update paths.
     */
    private boolean deferCrossMatch;

    /**
     * @param drainRightFirst {@code true} to drain {@link #pendingRight} before {@link #pendingLeft}
     *        in {@link #prepareForSettle()}.
     *        {@link AbstractIfExistsNode} passes {@code true}:
     *        its left reconcile is a full recompute for that left tuple
     *        (clear its whole tracker list, reset the counter to zero, re-walk the entire right side,
     *        fire one aggregate propagation decision),
     *        so running it last makes it authoritative over whatever the right pass did for the same
     *        left tuple in between,
     *        which is what makes {@code updateCounterRight(...)}'s pending-left skip safe.
     *        {@link AbstractJoinNode} passes {@code false}:
     *        whichever side is reconciled second always sees the first side's just-created out-tuple,
     *        so draining left first merely fixes which side's view is "first",
     *        without affecting correctness either way.
     */
    protected AbstractCrossMatchNode(TupleLifecycle<?> nextNodesTupleLifecycle, boolean isFiltering,
            InTupleStorePositionTracker tupleStorePositionTracker, boolean drainRightFirst) {
        super(nextNodesTupleLifecycle);
        this.isFiltering = isFiltering;
        this.drainRightFirst = drainRightFirst;
        if (isFiltering) {
            this.pendingLeftMarkerIndex = tupleStorePositionTracker.reserveNextLeft();
            var pendingLeftPrev = tupleStorePositionTracker.reserveNextLeft();
            var pendingLeftNext = tupleStorePositionTracker.reserveNextLeft();
            this.pendingLeft = new TupleList<>(pendingLeftPrev, pendingLeftNext);
            this.pendingLeftConsumer = leftTuple -> {
                leftTuple.setStore(pendingLeftMarkerIndex, null);
                reconcilePendingLeft(leftTuple);
            };
            this.pendingRightMarkerIndex = tupleStorePositionTracker.reserveNextRight();
            var pendingRightPrev = tupleStorePositionTracker.reserveNextRight();
            var pendingRightNext = tupleStorePositionTracker.reserveNextRight();
            this.pendingRight = new TupleList<>(pendingRightPrev, pendingRightNext);
            this.pendingRightConsumer = rightTuple -> {
                rightTuple.setStore(pendingRightMarkerIndex, null);
                reconcilePendingRight(rightTuple);
            };
        } else {
            // No-op; unused, avoids @Nullable.
            this.pendingLeftMarkerIndex = -1;
            this.pendingLeft = TupleList.EMPTY;
            this.pendingLeftConsumer = leftTuple -> {
            };
            this.pendingRightMarkerIndex = -1;
            this.pendingRight = TupleList.EMPTY;
            this.pendingRightConsumer = rightTuple -> {
            };
        }
    }

    /**
     * Runs {@code leftTuple}'s cross-match (the opposite-side walk).
     * When {@link #deferCrossMatch} says a stale read is possible here,
     * the walk is enqueued for this node's own layer turn instead of running now,
     * unless the tuple is already awaiting one;
     * otherwise it runs on the spot, guarded per read by {@link Tuple#getState()} checks.
     * Only called from filtering code paths.
     */
    protected final void crossMatchLeft(LeftTuple_ leftTuple) {
        if (!deferCrossMatch) {
            reconcilePendingLeft(leftTuple);
        } else if (leftTuple.getStore(pendingLeftMarkerIndex) == null) {
            leftTuple.setStore(pendingLeftMarkerIndex, Boolean.TRUE);
            pendingLeft.add(leftTuple);
        }
    }

    /**
     * The mirror image of {@link #crossMatchLeft}.
     */
    protected final void crossMatchRight(UniTuple<Right_> rightTuple) {
        if (!deferCrossMatch) {
            reconcilePendingRight(rightTuple);
        } else if (rightTuple.getStore(pendingRightMarkerIndex) == null) {
            rightTuple.setStore(pendingRightMarkerIndex, Boolean.TRUE);
            pendingRight.add(rightTuple);
        }
    }

    /**
     * Removes {@code leftTuple} from the pending queue, if it is on it:
     * a tuple can be retracted in the same round it was enqueued,
     * before its turn to reconcile ever comes.
     * Must run before the tuple's own store entries (composite key, out-tuple list, counter entry, ...)
     * are cleared,
     * since a still-pending entry left dangling would be read by {@link #prepareForSettle()}
     * after those are gone.
     */
    protected final void clearPendingLeft(LeftTuple_ leftTuple) {
        if (isPendingLeft(leftTuple)) {
            leftTuple.setStore(pendingLeftMarkerIndex, null);
            pendingLeft.remove(leftTuple);
        }
    }

    /**
     * The mirror image of {@link #clearPendingLeft}.
     */
    protected final void clearPendingRight(UniTuple<Right_> rightTuple) {
        if (pendingRightMarkerIndex >= 0 && rightTuple.getStore(pendingRightMarkerIndex) != null) {
            rightTuple.setStore(pendingRightMarkerIndex, null);
            pendingRight.remove(rightTuple);
        }
    }

    /**
     * Whether {@code leftTuple} is currently awaiting its own {@link #reconcilePendingLeft(Tuple)}
     * in this same {@link #prepareForSettle()} run.
     * Used by {@link AbstractIfExistsNode} to skip a left counter whose own reconcile,
     * running later in the same drain,
     * will recompute it from scratch against the now-settled right side anyway;
     * see the constructor's {@code drainRightFirst} documentation for why that skip is safe.
     */
    protected final boolean isPendingLeft(LeftTuple_ leftTuple) {
        return pendingLeftMarkerIndex >= 0 && leftTuple.getStore(pendingLeftMarkerIndex) != null;
    }

    @Override
    public final void setSettleDistance(long settleDistance) {
        this.settleDistance = settleDistance;
        recomputeDeferCrossMatch();
    }

    @Override
    public final void preloadStarted() {
        if (preloadingActive) {
            throw new IllegalStateException("Impossible state: node (%s) is already preloading.".formatted(this));
        } else {
            this.preloadingActive = true;
            recomputeDeferCrossMatch();
        }
    }

    @Override
    public final void preloadEnded() {
        if (!preloadingActive) {
            throw new IllegalStateException("Impossible state: node (%s) is not preloading.".formatted(this));
        } else {
            this.preloadingActive = false;
            recomputeDeferCrossMatch();
        }
    }

    private void recomputeDeferCrossMatch() {
        this.deferCrossMatch = canDeferWork() && !preloadingActive;
    }

    @Override
    public final boolean canDeferWork() {
        if (isFiltering && settleDistance < 0) {
            throw new IllegalStateException("Impossible state: the settle distance of node (%s) was never set."
                    .formatted(this));
        }
        // A filtering node whose inputs are closer than MIN_DEFER_DISTANCE can only ever read a tuple
        // that has already been told it is doomed, which the per-read isActive() guards catch.
        // Further apart, the doom may not have propagated that far yet; only then is deferral needed.
        return isFiltering && settleDistance >= MIN_DEFER_DISTANCE;
    }

    @Override
    public final void prepareForSettle() {
        if (!isFiltering) { // Non-filtering: nothing was ever enqueued.
            return;
        }
        if (drainRightFirst) {
            pendingRight.clear(pendingRightConsumer);
            pendingLeft.clear(pendingLeftConsumer);
        } else {
            pendingLeft.clear(pendingLeftConsumer);
            pendingRight.clear(pendingRightConsumer);
        }
    }

    /**
     * Re-runs this left tuple's cross-match against the current
     * (now fully settled, for this round) opposite side,
     * exactly as an eager filtering update already would have.
     * Reusing that logic is what makes this correct for a tuple that was actually a fresh insert too:
     * with nothing recorded for it yet, every match it finds is necessarily new.
     * Implemented further down because only the subclass knows both what to walk
     * (indexed: the shared index/bucket; unindexed: the plain tuple list)
     * and what to do with a match.
     */
    protected abstract void reconcilePendingLeft(LeftTuple_ leftTuple);

    /**
     * The mirror image of {@link #reconcilePendingLeft}.
     */
    protected abstract void reconcilePendingRight(UniTuple<Right_> rightTuple);

}
