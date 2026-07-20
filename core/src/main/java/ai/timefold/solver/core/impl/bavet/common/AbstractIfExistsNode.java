package ai.timefold.solver.core.impl.bavet.common;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.InTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
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
        extends AbstractTwoInputNode<LeftTuple_, UniTuple<Right_>> {

    protected final boolean shouldExist;

    // When isFiltering, these slots hold the head FilteringTracker of a hidden intrusive doubly-linked list
    // (null = empty list). Links are stored in FilteringTracker's own prev/next fields — there is no list object.
    // See FilteringTracker for the field layout.
    protected final int inputStoreIndexLeftTrackerList; // -1 if !isFiltering
    protected final int inputStoreIndexRightTrackerList; // -1 if !isFiltering

    protected final boolean isFiltering;
    private final DynamicPropagationQueue<LeftTuple_, ExistsCounter<LeftTuple_>> propagationQueue;

    protected AbstractIfExistsNode(boolean shouldExist, TupleLifecycle<LeftTuple_> nextNodesTupleLifecycle, boolean isFiltering,
            InTupleStorePositionTracker tupleStorePositionTracker) {
        super(nextNodesTupleLifecycle);
        this.shouldExist = shouldExist;
        this.inputStoreIndexLeftTrackerList = isFiltering ? tupleStorePositionTracker.reserveNextLeft() : -1;
        this.inputStoreIndexRightTrackerList = isFiltering ? tupleStorePositionTracker.reserveNextRight() : -1;
        this.isFiltering = isFiltering;
        this.propagationQueue = new DynamicPropagationQueue<>(nextNodesTupleLifecycle);
    }

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

    protected void incrementCounterRightWithoutIndictment(ExistsCounter<LeftTuple_> counter) {
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
        if (counter.countRight == 0) {
            if (shouldExist) {
                doInsertCounter(counter);
            } else {
                doRetractCounter(counter);
            }
        } // Else do not even propagate an update
        counter.getTuple().getIndictmentSupportForNodeId(getId())
                .add(Objects.requireNonNull(rightTuple.getA()));
        counter.countRight++;
    }

    protected void decrementCounterRightWithoutIndictment(ExistsCounter<LeftTuple_> counter) {
        counter.countRight--;
        if (counter.countRight == 0) {
            if (shouldExist) {
                doRetractCounter(counter);
            } else {
                doInsertCounter(counter);
            }
        } // Else do not even propagate an update
    }

    protected void decrementCounterRightUpdatingIndictment(ExistsCounter<LeftTuple_> counter, UniTuple<Right_> rightTuple) {
        counter.countRight--;
        counter.getTuple().getIndictmentSupportForNodeId(getId())
                .remove(Objects.requireNonNull(rightTuple.getA()));
        if (counter.countRight == 0) {
            if (shouldExist) {
                doRetractCounter(counter);
            } else {
                doInsertCounter(counter);
            }
        } // Else do not even propagate an update
    }

    // Prepends tracker into the left tuple's hidden intrusive tracker list.
    // The left tuple's store at inputStoreIndexLeftTrackerList holds the list head (null = empty).
    private void linkLeft(FilteringTracker<LeftTuple_> tracker) {
        var leftTuple = tracker.counter.leftTuple;
        FilteringTracker<LeftTuple_> head = leftTuple.getStore(inputStoreIndexLeftTrackerList);
        tracker.leftNext = head;
        if (head != null) {
            head.leftPrev = tracker;
        }
        leftTuple.setStore(inputStoreIndexLeftTrackerList, tracker);
    }

    // Prepends tracker into the right tuple's hidden intrusive tracker list.
    // The right tuple's store at inputStoreIndexRightTrackerList holds the list head (null = empty).
    private void linkRight(FilteringTracker<LeftTuple_> tracker) {
        var rightTuple = tracker.rightTuple;
        FilteringTracker<LeftTuple_> head = rightTuple.getStore(inputStoreIndexRightTrackerList);
        tracker.rightNext = head;
        if (head != null) {
            head.rightPrev = tracker;
        }
        rightTuple.setStore(inputStoreIndexRightTrackerList, tracker);
    }

    // Splices tracker out of its right tuple's hidden list (used when clearing from the left side).
    // Nulls the tracker's right links; if tracker is the head, updates the right tuple's slot.
    private void removeFromRight(FilteringTracker<LeftTuple_> tracker) {
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

    // Splices tracker out of its left tuple's hidden list (used when clearing from the right side).
    // Nulls the tracker's left links; if tracker is the head, updates the left tuple's slot.
    private void removeFromLeft(FilteringTracker<LeftTuple_> tracker) {
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

    // Clears the left tracker list rooted at leftTuple's inputStoreIndexLeftTrackerList slot,
    // cross-removing each tracker from its right tuple's hidden list. No-op when !isFiltering.
    // Walk safety: removeFromRight only touches right-side links, so leftNext is stable across the call.
    protected void clearLeftTrackerList(LeftTuple_ leftTuple) {
        if (!isFiltering) {
            return;
        }
        FilteringTracker<LeftTuple_> tracker = leftTuple.removeStore(inputStoreIndexLeftTrackerList);
        while (tracker != null) {
            var next = tracker.leftNext;
            removeFromRight(tracker);
            tracker = next;
        }
    }

    // Clears the right tracker list rooted at rightTuple's inputStoreIndexRightTrackerList slot,
    // decrementing each counter and cross-removing each tracker from its left tuple's hidden list.
    // Walk safety: removeFromLeft only touches left-side links, so rightNext is stable across the call.
    protected void clearRightTrackerList(UniTuple<Right_> rightTuple) {
        FilteringTracker<LeftTuple_> tracker = rightTuple.removeStore(inputStoreIndexRightTrackerList);

        if (rightTuple.getIndictmentSource() != IndictmentSource.DISABLED) {
            while (tracker != null) {
                var next = tracker.rightNext;
                decrementCounterRightUpdatingIndictment(tracker.counter, rightTuple);
                removeFromLeft(tracker);
                tracker = next;
            }
        } else {
            while (tracker != null) {
                var next = tracker.rightNext;
                decrementCounterRightWithoutIndictment(tracker.counter);
                removeFromLeft(tracker);
                tracker = next;
            }
        }
    }

    protected void updateCounterFromLeft(ExistsCounter<LeftTuple_> counter, UniTuple<Right_> rightTuple) {
        if (testFiltering(counter.leftTuple, rightTuple)) {
            counter.countRight++;
            var tracker = new FilteringTracker<>(counter, rightTuple);
            linkLeft(tracker);
            linkRight(tracker);
        }
    }

    protected void updateCounterFromRight(ExistsCounter<LeftTuple_> counter, UniTuple<Right_> rightTuple) {
        var leftTuple = counter.leftTuple;
        if (!leftTuple.getState().isActive()) {
            // Assume the following scenario:
            // - The operation is of two entities of the same type, both filtering out unassigned.
            // - One entity became unassigned, so the outTuple is getting retracted.
            // - The entity whose existence is being asserted is still assigned and is being updated.
            //
            // This means the filter would be called with (unassignedEntity, assignedEntity),
            // which breaks the expectation that the filter is only called on two assigned entities
            // and requires adding null checks to the filter for something that should intuitively be impossible.
            // We avoid this situation as it is clear that the outTuple must be retracted anyway,
            // and therefore any further updates to it are pointless.
            //
            // It is possible that the same problem would exist coming from the other side as well,
            // and therefore the right tuple would have to be checked for active state as well.
            // However, no such issue could have been reproduced; when in doubt, leave it out.
            return;
        }
        if (testFiltering(leftTuple, rightTuple)) {
            incrementCounterRightUpdatingIndictment(counter, rightTuple);
            var tracker = new FilteringTracker<>(counter, rightTuple);
            linkLeft(tracker);
            linkRight(tracker);
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
            // But even if right can produce tuples, it is not guaranteed to do so
            // and therefore the node needs to stay active.
            return true;
        }
    }

    @Override
    public Propagator getPropagator() {
        return propagationQueue;
    }

    @NullMarked
    protected static final class FilteringTracker<LeftTuple_ extends Tuple> {

        // A tracker is a node in TWO hidden intrusive doubly-linked lists at once:
        // one keyed on its left tuple (counter.leftTuple) and one on its right tuple.
        // The list heads live in the tuples' inputStoreIndexLeftTrackerList /
        // inputStoreIndexRightTrackerList store slots (null = empty list).
        // These fields ARE the links — no ElementAwareLinkedList or Entry is allocated.
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
