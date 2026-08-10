package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.InTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleList;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.indictment.IndictmentSource;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This class has two direct children: {@link AbstractIndexedIfExistsNode} and {@link AbstractUnindexedIfExistsNode}.
 * The logic in either is identical, except that the latter removes all indexing work.
 * Therefore any time that one of the classes changes,
 * the other should be inspected if it could benefit from applying the change there too.
 *
 * @param <LeftTuple_>
 * @param <Right_>
 */
public abstract class AbstractIfExistsNode<LeftTuple_ extends Tuple, Right_>
        extends AbstractTwoInputNode<LeftTuple_, UniTuple<Right_>>
        implements DeferredSettleAware {

    protected final boolean shouldExist;

    // When isFiltering, these slots hold the head FilteringTracker of a hidden intrusive doubly-linked list
    // (null = empty list). Links are stored in FilteringTracker's own prev/next fields — there is no list object.
    // See FilteringTracker for the field layout.
    protected final int inputStoreIndexLeftTrackerList; // -1 if !isFiltering
    protected final int inputStoreIndexRightTrackerList; // -1 if !isFiltering

    protected final boolean isFiltering;
    private final DynamicPropagationQueue<LeftTuple_, ExistsCounter<LeftTuple_>> propagationQueue;

    /**
     * Filtering ifExists/ifNotExists nodes defer their cross-match computation
     * (the opposite-side read)
     * from "whenever a parent propagates in" to this node's own layer turn
     * (see {@link #prepareForSettle()}),
     * closing the stale-activity race at its root instead of guarding against it per read;
     * the same mechanism {@code AbstractJoinNode} uses for filtering joins.
     * Non-filtering ifExists/ifNotExists never dereferences a fact through a user predicate
     * (it only counts index members),
     * so a stale-but-"active" read there can't corrupt anything;
     * these fields stay {@code null}/{@code -1} and cost nothing for them.
     * <p>
     * {@code pendingLeft}/{@code pendingRight} hold tuples whose cross-match is due;
     * the marker slots exist purely to make enqueueing idempotent.
     * Both lists are drained in {@link #prepareForSettle()},
     * calling {@link #reconcilePendingLeft(Tuple)}/{@link #reconcilePendingRight(UniTuple)};
     * a brand-new counter has an empty tracker list and {@code countRight == 0},
     * so it doubles as the insert path too.
     * <p>
     * Unlike the join sibling, which drains left before right,
     * this node drains <b>right before left</b>:
     * the left reconcile is a full recompute for that left tuple
     * (clear its whole tracker list, reset {@code countRight} to zero, re-walk the entire right side,
     * fire one aggregate propagation decision),
     * so running it last makes it authoritative over
     * whatever the right pass did for the same left tuple in between.
     * That in turn is what makes {@link #updateCounterRight(ExistsCounter, UniTuple)}'s pending-left skip safe:
     * skipping a left tuple's counter during the right pass only ever discards work
     * the left tuple's own reconcile would have superseded anyway.
     * The skip is self-protecting either way;
     * {@link #prepareForSettle()} clears each side's marker as it drains,
     * so swapping the two drains would just make the skip check see no pending markers left to skip,
     * degrading to correct-but-redundant work rather than an incorrect answer.
     * Only the optimisation depends on the order, never correctness.
     */
    private final int pendingLeftMarkerIndex;
    private final int pendingRightMarkerIndex;
    protected final @Nullable TupleList<LeftTuple_> pendingLeft;
    protected final @Nullable TupleList<UniTuple<Right_>> pendingRight;

    protected AbstractIfExistsNode(boolean shouldExist, TupleLifecycle<LeftTuple_> nextNodesTupleLifecycle, boolean isFiltering,
            InTupleStorePositionTracker tupleStorePositionTracker) {
        super(nextNodesTupleLifecycle);
        this.shouldExist = shouldExist;
        this.inputStoreIndexLeftTrackerList = isFiltering ? tupleStorePositionTracker.reserveNextLeft() : -1;
        this.inputStoreIndexRightTrackerList = isFiltering ? tupleStorePositionTracker.reserveNextRight() : -1;
        this.isFiltering = isFiltering;
        this.propagationQueue = new DynamicPropagationQueue<>(nextNodesTupleLifecycle);
        if (isFiltering) {
            this.pendingLeftMarkerIndex = tupleStorePositionTracker.reserveNextLeft();
            var pendingLeftPrev = tupleStorePositionTracker.reserveNextLeft();
            var pendingLeftNext = tupleStorePositionTracker.reserveNextLeft();
            this.pendingLeft = new TupleList<>(pendingLeftPrev, pendingLeftNext);
            this.pendingRightMarkerIndex = tupleStorePositionTracker.reserveNextRight();
            var pendingRightPrev = tupleStorePositionTracker.reserveNextRight();
            var pendingRightNext = tupleStorePositionTracker.reserveNextRight();
            this.pendingRight = new TupleList<>(pendingRightPrev, pendingRightNext);
        } else {
            this.pendingLeftMarkerIndex = -1;
            this.pendingLeft = null;
            this.pendingRightMarkerIndex = -1;
            this.pendingRight = null;
        }
    }

    /**
     * Enqueues {@code leftTuple} for cross-match reconciliation at this node's own layer turn,
     * unless it is already awaiting one.
     * Only called from filtering code paths.
     */
    protected final void enqueuePendingLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(pendingLeftMarkerIndex) == null) {
            leftTuple.setStore(pendingLeftMarkerIndex, Boolean.TRUE);
            pendingLeft.add(leftTuple);
        }
    }

    /**
     * The mirror image of {@link #enqueuePendingLeft}.
     */
    protected final void enqueuePendingRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(pendingRightMarkerIndex) == null) {
            rightTuple.setStore(pendingRightMarkerIndex, Boolean.TRUE);
            pendingRight.add(rightTuple);
        }
    }

    /**
     * Removes {@code leftTuple} from the pending queue, if it is on it:
     * a tuple can be retracted in the same round it was enqueued,
     * before its turn to reconcile ever comes.
     * Must run before the tuple's own store entries (composite key, counter entry, ...) are cleared,
     * since a still-pending entry left dangling would be read by {@link #prepareForSettle()} after those are gone.
     */
    protected final void clearPendingLeft(LeftTuple_ leftTuple) {
        if (pendingLeft != null && leftTuple.getStore(pendingLeftMarkerIndex) != null) {
            leftTuple.setStore(pendingLeftMarkerIndex, null);
            pendingLeft.remove(leftTuple);
        }
    }

    /**
     * The mirror image of {@link #clearPendingLeft}.
     */
    protected final void clearPendingRight(UniTuple<Right_> rightTuple) {
        if (pendingRight != null && rightTuple.getStore(pendingRightMarkerIndex) != null) {
            rightTuple.setStore(pendingRightMarkerIndex, null);
            pendingRight.remove(rightTuple);
        }
    }

    /**
     * Whether {@code leftTuple} is currently awaiting its own {@link #reconcilePendingLeft(Tuple)}
     * in this same {@link #prepareForSettle()} run.
     * Used by {@link #updateCounterRight(ExistsCounter, UniTuple)} to skip a left counter whose own reconcile,
     * running later in the same drain,
     * will recompute it from scratch against the now-settled right side anyway;
     * see this class's own pending-fields javadoc for why draining right before left is what makes the skip safe.
     */
    protected final boolean isPendingLeft(LeftTuple_ leftTuple) {
        return pendingLeft != null && leftTuple.getStore(pendingLeftMarkerIndex) != null;
    }

    @Override
    public final boolean canDeferWork() {
        return isFiltering;
    }

    @Override
    public final void prepareForSettle() {
        if (pendingLeft == null) { // Non-filtering: nothing was ever enqueued.
            return;
        }
        pendingRight.clear(rightTuple -> {
            rightTuple.setStore(pendingRightMarkerIndex, null);
            reconcilePendingRight(rightTuple);
        });
        pendingLeft.clear(leftTuple -> {
            leftTuple.setStore(pendingLeftMarkerIndex, null);
            reconcilePendingLeft(leftTuple);
        });
    }

    /**
     * Re-runs this left tuple's cross-match against the current (now fully settled, for this round)
     * opposite side,
     * exactly as an eager filtering update already would have;
     * clear its tracker list, reset {@code countRight} to zero, re-walk the whole right side,
     * then fire one aggregate {@link #updateCounterLeft(ExistsCounter)}.
     * Reusing that logic is what makes this correct for a tuple that was actually a fresh insert too:
     * a brand-new counter has an empty tracker list, so every match found is necessarily new.
     * Implemented by each subclass because only it knows how to walk the opposite side
     * (indexed: the shared index/bucket; unindexed: the plain tuple list).
     */
    protected abstract void reconcilePendingLeft(LeftTuple_ leftTuple);

    /**
     * The mirror image of {@link #reconcilePendingLeft}:
     * clear this right tuple's tracker list,
     * then re-walk the left counters that share its composite key via {@link #updateCounterRight}.
     */
    protected abstract void reconcilePendingRight(UniTuple<Right_> rightTuple);

    @Override
    public StreamKind getStreamKind() {
        return StreamKind.IF_EXISTS;
    }

    protected abstract boolean testFiltering(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple);

    protected void initCounterLeft(ExistsCounter<LeftTuple_> counter) {
        if (shouldExist ? counter.countRight > 0 : counter.countRight == 0) {
            // Counters start out dead
            propagationQueue.insert(counter);
        }
    }

    protected final void updateUnchangedCounterLeft(ExistsCounter<LeftTuple_> counter) {
        if (counter.state != TupleState.OK) {
            // Counter state does not change because the index keys didn't change
            return;
        }
        // Still needed to propagate the update for downstream filters, matchWeighers, ...
        propagationQueue.update(counter);
    }

    protected void updateCounterLeft(ExistsCounter<LeftTuple_> counter) {
        var state = counter.state;
        if (shouldExist ? counter.countRight > 0 : counter.countRight == 0) {
            // Insert or update
            switch (state) {
                case CREATING, UPDATING -> {
                    // Don't add the tuple to the propagation queue twice
                }
                case OK, DYING -> propagationQueue.update(counter);
                case DEAD, ABORTING -> propagationQueue.insert(counter);
                default ->
                    throw new IllegalStateException("Impossible state: the counter (%s) has an impossible insert state (%s)."
                            .formatted(counter, state));
            }
        } else {
            // Retract or remain dead
            if (!state.isActive()) {
                // Don't add the tuple to the propagation queue twice.
                return;
            }
            switch (state) {
                case CREATING -> // Kill it before it propagates.
                    propagationQueue.retract(counter, TupleState.ABORTING);
                case OK, UPDATING -> // Kill the original propagation.
                    propagationQueue.retract(counter, TupleState.DYING);
                default ->
                    throw new IllegalStateException("Impossible state: The counter (%s) has an impossible retract state (%s)."
                            .formatted(counter, state));

            }
        }
    }

    protected void killCounterLeft(ExistsCounter<LeftTuple_> counter) {
        if (shouldExist ? counter.countRight > 0 : counter.countRight == 0) {
            doRetractCounter(counter);
        }
    }

    protected void incrementCounterRight(ExistsCounter<LeftTuple_> counter) {
        if (counter.countRight == 0) {
            if (shouldExist) {
                doInsertCounter(counter);
            } else {
                doRetractCounter(counter);
            }
        } // Else do not even propagate an update
        counter.countRight++;
    }

    protected void incrementCounterRightUpdatingIndictment(ExistsCounter<LeftTuple_> counter, UniTuple<Right_> rightTuple) {
        IndictmentSource.addSupport(getId(), counter.getTuple(), rightTuple);
        if (counter.countRight == 0) {
            if (shouldExist) {
                doInsertCounter(counter);
            } else {
                doRetractCounter(counter);
            }
        } else if (shouldExist) {
            doUpdateCounter(counter);
        }
        counter.countRight++;
    }

    protected void decrementCounterRight(ExistsCounter<LeftTuple_> counter) {
        counter.countRight--;
        if (counter.countRight == 0) {
            if (shouldExist) {
                doRetractCounter(counter);
            } else {
                doInsertCounter(counter);
            }
        } else {
            // count != 0, so only propagate if we are in an `ifExists`
            if (shouldExist) {
                doUpdateCounter(counter);
            }
        } // Else do not even propagate an update
    }

    protected void decrementCounterRightUpdatingIndictment(ExistsCounter<LeftTuple_> counter, UniTuple<Right_> rightTuple) {
        IndictmentSource.removeSupport(getId(), counter.getTuple(), rightTuple);
        counter.countRight--;
        if (counter.countRight == 0) {
            if (shouldExist) {
                doRetractCounter(counter);
            } else {
                doInsertCounter(counter);
            }
        } else if (shouldExist) {
            doUpdateCounter(counter);
        }
    }

    /**
     * Clears the left tracker list rooted at leftTuple's inputStoreIndexLeftTrackerList slot,
     * cross-removing each tracker from its right tuple's hidden list.
     * No-op when !isFiltering.
     * Walk safety: {@link #removeRight(FilteringTracker)} only touches right-side links,
     * so {@code leftNext} is stable across the call.
     */
    protected void clearLeftTrackerList(LeftTuple_ leftTuple) {
        if (!isFiltering) {
            return;
        }
        FilteringTracker<LeftTuple_> tracker = leftTuple.removeStore(inputStoreIndexLeftTrackerList);
        while (tracker != null) {
            var next = tracker.leftNext;
            removeRight(tracker);
            tracker = next;
        }
    }

    /**
     * Splices tracker out of its right tuple's hidden list
     * (used when clearing from the left side).
     * Nulls the tracker's right links;
     * if tracker is the head, updates the right tuple's slot.
     */
    private void removeRight(FilteringTracker<LeftTuple_> tracker) {
        var prev = tracker.rightPrev;
        var next = tracker.rightNext;
        if (prev != null) {
            prev.rightNext = next;
        } else {
            // tracker is the head of the right list; update the slot
            tracker.rightTuple.setStore(inputStoreIndexRightTrackerList, next);
        }
        if (next != null) {
            next.rightPrev = prev;
        }
        tracker.rightPrev = null;
        tracker.rightNext = null;
    }

    /**
     * Clears the right tracker list rooted at rightTuple's inputStoreIndexRightTrackerList slot,
     * decrementing each counter and cross-removing each tracker from its left tuple's hidden list.
     * Walk safety: removeFromLeft only touches left-side links, so rightNext is stable across the call.
     */
    protected void clearRightTrackerList(UniTuple<Right_> rightTuple) {
        FilteringTracker<LeftTuple_> tracker = rightTuple.removeStore(inputStoreIndexRightTrackerList);
        if (rightTuple.getIndictmentSource() == IndictmentSource.DISABLED) {
            while (tracker != null) {
                var next = tracker.rightNext;
                decrementCounterRight(tracker.counter);
                removeLeft(tracker);
                tracker = next;
            }
        } else {
            while (tracker != null) {
                var next = tracker.rightNext;
                decrementCounterRightUpdatingIndictment(tracker.counter, rightTuple);
                removeLeft(tracker);
                tracker = next;
            }
        }
    }

    /**
     * Splices tracker out of its left tuple's hidden list (used when clearing from the right side).
     * Nulls the tracker's left links; if tracker is the head, updates the left tuple's slot.
     */
    private void removeLeft(FilteringTracker<LeftTuple_> tracker) {
        var prev = tracker.leftPrev;
        var next = tracker.leftNext;
        if (prev != null) {
            prev.leftNext = next;
        } else {
            // tracker is the head of the left list; update the slot
            tracker.counter.leftTuple.setStore(inputStoreIndexLeftTrackerList, next);
        }
        if (next != null) {
            next.leftPrev = prev;
        }
        tracker.leftPrev = null;
        tracker.leftNext = null;
    }

    protected void updateCounterLeft(ExistsCounter<LeftTuple_> counter, UniTuple<Right_> rightTuple) {
        // This only ever runs from reconcilePendingLeft, at this node's own layer turn,
        // after every ancestor on both sides has completed its retract/update/insert turn for this round;
        // rightTuple can no longer be stale here, so no per-read staleness check is needed.
        if (testFiltering(counter.leftTuple, rightTuple)) {
            counter.countRight++;
            IndictmentSource.addSupport(getId(), counter.getTuple(), rightTuple);
            var tracker = new FilteringTracker<>(counter, rightTuple);
            linkLeft(tracker);
            linkRight(tracker);
        } else {
            IndictmentSource.removeSupport(getId(), counter.getTuple(), rightTuple);
        }
    }

    /**
     * Prepends tracker into the left tuple's hidden intrusive tracker list.
     * The left tuple's store at {@link #inputStoreIndexLeftTrackerList} holds the list head (null = empty).
     */
    private void linkLeft(FilteringTracker<LeftTuple_> tracker) {
        var leftTuple = tracker.counter.leftTuple;
        FilteringTracker<LeftTuple_> head = leftTuple.getStore(inputStoreIndexLeftTrackerList);
        tracker.leftNext = head;
        if (head != null) {
            head.leftPrev = tracker;
        }
        leftTuple.setStore(inputStoreIndexLeftTrackerList, tracker);
    }

    /**
     * Prepends tracker into the right tuple's hidden intrusive tracker list.
     * The right tuple's store at {@link #inputStoreIndexRightTrackerList} holds the list head (null = empty).
     */
    private void linkRight(FilteringTracker<LeftTuple_> tracker) {
        var rightTuple = tracker.rightTuple;
        FilteringTracker<LeftTuple_> head = rightTuple.getStore(inputStoreIndexRightTrackerList);
        tracker.rightNext = head;
        if (head != null) {
            head.rightPrev = tracker;
        }
        rightTuple.setStore(inputStoreIndexRightTrackerList, tracker);
    }

    protected void updateCounterRight(ExistsCounter<LeftTuple_> counter, UniTuple<Right_> rightTuple) {
        var leftTuple = counter.leftTuple;
        if (isPendingLeft(leftTuple)) {
            // This left tuple's own reconcile, later in this same prepareForSettle drain,
            // recomputes its entire tracker set against the settled right side, including this right tuple.
            // Skipping here is what makes that recompute the only work done for the pair,
            // instead of the second.
            // See this class's pending-fields javadoc for why draining right before left
            // is what makes this safe rather than merely an optimisation that happens to hold.
            return;
        }
        // This only ever runs from reconcilePendingRight, at this node's own layer turn,
        // after every ancestor on both sides has completed its retract/update/insert turn for this round;
        // leftTuple can no longer be stale here
        // (the pending-left skip above already handles the one case where leftTuple's own reconcile hasn't run yet this round),
        // so no per-read staleness check is needed.
        if (testFiltering(leftTuple, rightTuple)) {
            incrementCounterRight(counter);
            IndictmentSource.addSupport(getId(), counter.getTuple(), rightTuple);
            var tracker = new FilteringTracker<>(counter, rightTuple);
            linkLeft(tracker);
            linkRight(tracker);
        } else {
            IndictmentSource.removeSupport(getId(), counter.getTuple(), rightTuple);
        }
    }

    private void doInsertCounter(ExistsCounter<LeftTuple_> counter) {
        switch (counter.state) {
            case DYING -> propagationQueue.update(counter);
            case DEAD, ABORTING -> propagationQueue.insert(counter);
            default -> throw new IllegalStateException("Impossible state: the counter (%s) has an impossible insert state (%s)."
                    .formatted(counter, counter.state));
        }
    }

    private void doRetractCounter(ExistsCounter<LeftTuple_> counter) {
        switch (counter.state) {
            case CREATING -> // Kill it before it propagates.
                propagationQueue.retract(counter, TupleState.ABORTING);
            case OK, UPDATING -> // Kill the original propagation.
                propagationQueue.retract(counter, TupleState.DYING);
            default ->
                throw new IllegalStateException("Impossible state: The counter (%s) has an impossible retract state (%s)."
                        .formatted(counter, counter.state));
        }
    }

    private void doUpdateCounter(ExistsCounter<LeftTuple_> counter) {
        switch (counter.state) {
            case DYING, OK, UPDATING, CREATING -> propagationQueue.update(counter);
            case DEAD, ABORTING -> propagationQueue.insert(counter);
            default -> throw new IllegalStateException("Impossible state: the counter (%s) has an impossible insert state (%s)."
                    .formatted(counter, counter.state));
        }
    }

    @Override
    protected boolean canProduceTuples() {
        // The left input must produce tuples no matter what,
        // otherwise ifExists has nothing to join with.
        if (!leftCanProduceTuples) {
            return false;
        } else if (shouldExist) {
            // For the ifExists case, the right input must produce tuples as well,
            // otherwise no left tuple can ever match.
            return rightCanProduceTuples;
        } else {
            // For the ifNotExists case, if the right can not produce tuples, this node will.
            // But even if right can produce tuples,
            // it is not guaranteed to do so
            // and therefore the node needs to stay active.
            return true;
        }
    }

    @Override
    public Propagator getPropagator() {
        return propagationQueue;
    }

    /**
     * A node in two hidden intrusive doubly-linked lists at once:
     * one keyed on its left tuple (counter.leftTuple) and one on its right tuple.
     * The list heads live in the tuples' inputStoreIndexLeftTrackerList /
     * inputStoreIndexRightTrackerList store slots (null = empty list).
     * These fields are the links — no ElementAwareLinkedList or Entry is allocated.
     */
    @NullMarked
    protected static final class FilteringTracker<LeftTuple_ extends Tuple> {

        final ExistsCounter<LeftTuple_> counter; // -> leftTuple, for the left-keyed list and counter decrement
        final Tuple rightTuple; // for the right-keyed list; typed as Tuple (not UniTuple<Right_>) — only getStore/setStore needed
        @Nullable
        FilteringTracker<LeftTuple_> leftPrev, leftNext; // links in the left tuple's hidden list
        @Nullable
        FilteringTracker<LeftTuple_> rightPrev, rightNext; // links in the right tuple's hidden list

        FilteringTracker(ExistsCounter<LeftTuple_> counter, Tuple rightTuple) {
            this.counter = counter;
            this.rightTuple = rightTuple;
        }

    }

}
