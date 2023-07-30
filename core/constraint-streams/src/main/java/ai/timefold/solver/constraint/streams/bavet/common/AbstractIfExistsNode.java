package ai.timefold.solver.constraint.streams.bavet.common;

import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.ABORTING;
import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.CREATING;
import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.DYING;
import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.UPDATING;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.ElementAwareList;
import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

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
        extends AbstractNode
        implements LeftTupleLifecycle<LeftTuple_>, RightTupleLifecycle<UniTuple<Right_>> {

    protected final boolean shouldExist;

    protected final int inputStoreIndexLeftTrackerList; // -1 if !isFiltering
    protected final int inputStoreIndexRightTrackerList; // -1 if !isFiltering

    protected final boolean isFiltering;
    private final IfExistsPropagationQueue<LeftTuple_> propagationQueue;

    protected AbstractIfExistsNode(boolean shouldExist,
            int inputStoreIndexLeftPropagationQueueMetadata,
            int inputStoreIndexLeftTrackerList, int inputStoreIndexRightTrackerList,
            TupleLifecycle<LeftTuple_> nextNodesTupleLifecycle,
            boolean isFiltering) {
        this.shouldExist = shouldExist;
        this.inputStoreIndexLeftTrackerList = inputStoreIndexLeftTrackerList;
        this.inputStoreIndexRightTrackerList = inputStoreIndexRightTrackerList;
        this.isFiltering = isFiltering;
        this.propagationQueue =
                new IfExistsPropagationQueue<>(nextNodesTupleLifecycle, inputStoreIndexLeftPropagationQueueMetadata);
    }

    protected abstract boolean testFiltering(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple);

    protected void initCounterLeft(ExistsCounter<LeftTuple_> counter) {
        if (shouldExist ? counter.countRight > 0 : counter.countRight == 0) {
            // Counters start out dead
            propagationQueue.insert(counter, CREATING);
        }
    }

    protected final void updateUnchangedCounterLeft(ExistsCounter<LeftTuple_> counter) {
        if (counter.state != TupleState.OK) {
            // Counter state does not change because the index properties didn't change
            return;
        }
        // Still needed to propagate the update for downstream filters, matchWeighers, ...
        propagationQueue.update(counter, UPDATING);
    }

    protected void updateCounterLeft(ExistsCounter<LeftTuple_> counter) {
        TupleState state = counter.state;
        if (shouldExist ? counter.countRight > 0 : counter.countRight == 0) {
            // Insert or update
            switch (state) {
                case CREATING, UPDATING -> {
                    // Don't add the tuple to the propagation queue twice
                }
                case OK, DYING -> propagationQueue.update(counter, UPDATING);
                case DEAD, ABORTING -> propagationQueue.insert(counter, CREATING);
                default -> throw new IllegalStateException("Impossible state: the counter (" + counter
                        + ") has an impossible insert state (" + state + ").");
            }
        } else {
            // Retract or remain dead
            if (!state.isActive()) {
                // Don't add the tuple to the propagation queue twice.
                return;
            }
            switch (state) {
                case CREATING -> // Kill it before it propagates.
                    propagationQueue.retract(counter, ABORTING);
                case OK, UPDATING -> // Kill the original propagation.
                    propagationQueue.retract(counter, DYING);
                default -> throw new IllegalStateException("Impossible state: The counter (" + counter
                        + ") has an impossible retract state (" + state + ").");

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

    protected ElementAwareList<FilteringTracker<LeftTuple_>> updateRightTrackerList(UniTuple<Right_> rightTuple) {
        ElementAwareList<FilteringTracker<LeftTuple_>> rightTrackerList = rightTuple.getStore(inputStoreIndexRightTrackerList);
        rightTrackerList.forEach(filteringTacker -> {
            decrementCounterRight(filteringTacker.counter);
            filteringTacker.remove();
        });
        return rightTrackerList;
    }

    protected void updateCounterFromLeft(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple, ExistsCounter<LeftTuple_> counter,
            ElementAwareList<FilteringTracker<LeftTuple_>> leftTrackerList) {
        if (testFiltering(leftTuple, rightTuple)) {
            counter.countRight++;
            ElementAwareList<FilteringTracker<LeftTuple_>> rightTrackerList =
                    rightTuple.getStore(inputStoreIndexRightTrackerList);
            new FilteringTracker<>(counter, leftTrackerList, rightTrackerList);
        }
    }

    protected void updateCounterFromRight(UniTuple<Right_> rightTuple, ExistsCounter<LeftTuple_> counter,
            ElementAwareList<FilteringTracker<LeftTuple_>> rightTrackerList) {
        if (testFiltering(counter.leftTuple, rightTuple)) {
            incrementCounterRight(counter);
            ElementAwareList<FilteringTracker<LeftTuple_>> leftTrackerList =
                    counter.leftTuple.getStore(inputStoreIndexLeftTrackerList);
            new FilteringTracker<>(counter, leftTrackerList, rightTrackerList);
        }
    }

    private void doInsertCounter(ExistsCounter<LeftTuple_> counter) {
        switch (counter.state) {
            case DYING -> propagationQueue.update(counter, UPDATING);
            case DEAD, ABORTING -> propagationQueue.insert(counter, CREATING);
            default -> throw new IllegalStateException("Impossible state: the counter (" + counter
                    + ") has an impossible insert state (" + counter.state + ").");
        }
    }

    private void doRetractCounter(ExistsCounter<LeftTuple_> counter) {
        switch (counter.state) {
            case CREATING -> // Kill it before it propagates.
                propagationQueue.retract(counter, ABORTING);
            case OK, UPDATING -> // Kill the original propagation.
                propagationQueue.retract(counter, DYING);
            default -> throw new IllegalStateException("Impossible state: The counter (" + counter
                    + ") has an impossible retract state (" + counter.state + ").");
        }
    }

    @Override
    public final void calculateScore() {
        propagationQueue.calculateScore(this);
    }

    protected static final class FilteringTracker<LeftTuple_ extends AbstractTuple> {
        final ExistsCounter<LeftTuple_> counter;
        private final ElementAwareListEntry<FilteringTracker<LeftTuple_>> leftTrackerEntry;
        private final ElementAwareListEntry<FilteringTracker<LeftTuple_>> rightTrackerEntry;

        FilteringTracker(ExistsCounter<LeftTuple_> counter, ElementAwareList<FilteringTracker<LeftTuple_>> leftTrackerList,
                ElementAwareList<FilteringTracker<LeftTuple_>> rightTrackerList) {
            this.counter = counter;
            leftTrackerEntry = leftTrackerList.add(this);
            rightTrackerEntry = rightTrackerList.add(this);
        }

        public void remove() {
            leftTrackerEntry.remove();
            rightTrackerEntry.remove();
        }

    }

}
