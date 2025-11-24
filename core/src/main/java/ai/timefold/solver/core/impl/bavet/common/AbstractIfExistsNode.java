package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.InTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.ElementAwareLinkedList;

import org.jspecify.annotations.NullMarked;

/**
 * This class has two direct children: {@link AbstractIndexedIfExistsNode} and {@link AbstractUnindexedIfExistsNode}.
 * The logic in either is identical, except that the latter removes all indexing work.
 * Therefore any time that one of the classes changes,
 * the other should be inspected if it could benefit from applying the change there too.
 *
 * @param <LeftTuple_>
 * @param <Right_>
 */
public abstract class AbstractIfExistsNode<LeftTuple_ extends AbstractTuple, Right_>
        extends AbstractTwoInputNode<LeftTuple_, UniTuple<Right_>> {

    protected final boolean shouldExist;

    protected final int inputStoreIndexLeftTrackerList; // -1 if !isFiltering
    protected final int inputStoreIndexRightTrackerList; // -1 if !isFiltering

    protected final boolean isFiltering;
    private final DynamicPropagationQueue<LeftTuple_, ExistsCounter<LeftTuple_>> propagationQueue;

    protected AbstractIfExistsNode(boolean shouldExist, TupleLifecycle<LeftTuple_> nextNodesTupleLifecycle, boolean isFiltering,
            InTupleStorePositionTracker tupleStorePositionTracker) {
        this.shouldExist = shouldExist;
        this.inputStoreIndexLeftTrackerList = isFiltering ? tupleStorePositionTracker.reserveNextLeft() : -1;
        this.inputStoreIndexRightTrackerList = isFiltering ? tupleStorePositionTracker.reserveNextRight() : -1;
        this.isFiltering = isFiltering;
        this.propagationQueue = new DynamicPropagationQueue<>(nextNodesTupleLifecycle);
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
        TupleState state = counter.state;
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

    protected void decrementCounterRight(ExistsCounter<LeftTuple_> counter) {
        counter.countRight--;
        if (counter.countRight == 0) {
            if (shouldExist) {
                doRetractCounter(counter);
            } else {
                doInsertCounter(counter);
            }
        } // Else do not even propagate an update
    }

    protected ElementAwareLinkedList<FilteringTracker<LeftTuple_>> updateRightTrackerList(UniTuple<Right_> rightTuple) {
        ElementAwareLinkedList<FilteringTracker<LeftTuple_>> rightTrackerList =
                rightTuple.getStore(inputStoreIndexRightTrackerList);
        rightTrackerList.clear(tracker -> {
            decrementCounterRight(tracker.counter);
            tracker.removeByRight();
        });
        return rightTrackerList;
    }

    protected void updateCounterFromLeft(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple, ExistsCounter<LeftTuple_> counter,
            ElementAwareLinkedList<FilteringTracker<LeftTuple_>> leftTrackerList) {
        if (testFiltering(leftTuple, rightTuple)) {
            counter.countRight++;
            new FilteringTracker<>(counter, leftTrackerList, rightTuple.getStore(inputStoreIndexRightTrackerList));
        }
    }

    protected void updateCounterFromRight(UniTuple<Right_> rightTuple, ExistsCounter<LeftTuple_> counter,
            ElementAwareLinkedList<FilteringTracker<LeftTuple_>> rightTrackerList) {
        var leftTuple = counter.leftTuple;
        if (!leftTuple.state.isActive()) {
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
            incrementCounterRight(counter);
            new FilteringTracker<>(counter, leftTuple.getStore(inputStoreIndexLeftTrackerList), rightTrackerList);
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
    public Propagator getPropagator() {
        return propagationQueue;
    }

    @NullMarked
    protected static final class FilteringTracker<LeftTuple_ extends AbstractTuple> {
        final ExistsCounter<LeftTuple_> counter;
        private final ElementAwareLinkedList.Entry<FilteringTracker<LeftTuple_>> leftTrackerEntry;
        private final ElementAwareLinkedList.Entry<FilteringTracker<LeftTuple_>> rightTrackerEntry;

        FilteringTracker(ExistsCounter<LeftTuple_> counter,
                ElementAwareLinkedList<FilteringTracker<LeftTuple_>> leftTrackerList,
                ElementAwareLinkedList<FilteringTracker<LeftTuple_>> rightTrackerList) {
            this.counter = counter;
            leftTrackerEntry = leftTrackerList.add(this);
            rightTrackerEntry = rightTrackerList.add(this);
        }

        public void removeByLeft() {
            rightTrackerEntry.remove();
        }

        public void removeByRight() {
            leftTrackerEntry.remove();
        }

    }

}
