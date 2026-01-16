package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.InTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.ElementAwareLinkedList;

/**
 * There is a strong likelihood that any change made to this class
 * should also be made to {@link AbstractIndexedIfExistsNode}.
 *
 * @param <LeftTuple_>
 * @param <Right_>
 */
public abstract class AbstractUnindexedIfExistsNode<LeftTuple_ extends Tuple, Right_>
        extends AbstractIfExistsNode<LeftTuple_, Right_>
        implements LeftTupleLifecycle<LeftTuple_>, RightTupleLifecycle<UniTuple<Right_>> {

    private final int inputStoreIndexLeftCounterEntry;

    private final int inputStoreIndexRightEntry;

    // Acts as a leftTupleList too
    private final ElementAwareLinkedList<ExistsCounter<LeftTuple_>> counterList = new ElementAwareLinkedList<>();
    private final ElementAwareLinkedList<UniTuple<Right_>> rightTupleList = new ElementAwareLinkedList<>();

    protected AbstractUnindexedIfExistsNode(boolean shouldExist, TupleLifecycle<LeftTuple_> nextNodesTupleLifecycle,
            boolean isFiltering, InTupleStorePositionTracker tupleStorePositionTracker) {
        super(shouldExist, nextNodesTupleLifecycle, isFiltering, tupleStorePositionTracker);
        this.inputStoreIndexLeftCounterEntry = tupleStorePositionTracker.reserveNextLeft();
        this.inputStoreIndexRightEntry = tupleStorePositionTracker.reserveNextRight();
    }

    @Override
    public final void insertLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftCounterEntry) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(leftTuple));
        }
        var counter = new ExistsCounter<>(leftTuple);
        leftTuple.setStore(inputStoreIndexLeftCounterEntry, counterList.add(counter));

        if (!isFiltering) {
            counter.countRight = rightTupleList.size();
        } else {
            var leftTrackerList = new ElementAwareLinkedList<FilteringTracker<LeftTuple_>>();
            for (var rightTuple : rightTupleList) {
                updateCounterFromLeft(counter, rightTuple, leftTrackerList);
            }
            leftTuple.setStore(inputStoreIndexLeftTrackerList, leftTrackerList);
        }
        initCounterLeft(counter);
    }

    @Override
    public final void updateLeft(LeftTuple_ leftTuple) {
        ElementAwareLinkedList.Entry<ExistsCounter<LeftTuple_>> counterEntry =
                leftTuple.getStore(inputStoreIndexLeftCounterEntry);
        if (counterEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertLeft(leftTuple);
            return;
        }
        var counter = counterEntry.element();
        // The indexers contain counters in the DEAD state, to track the rightCount.
        if (!isFiltering) {
            updateUnchangedCounterLeft(counter);
        } else {
            // Call filtering for the leftTuple and rightTuple combinations again
            ElementAwareLinkedList<FilteringTracker<LeftTuple_>> leftTrackerList =
                    leftTuple.getStore(inputStoreIndexLeftTrackerList);
            leftTrackerList.clear(FilteringTracker::removeByLeft);
            counter.countRight = 0;
            for (var rightTuple : rightTupleList) {
                updateCounterFromLeft(counter, rightTuple, leftTrackerList);
            }
            updateCounterLeft(counter);
        }
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        ElementAwareLinkedList.Entry<ExistsCounter<LeftTuple_>> counterEntry =
                leftTuple.removeStore(inputStoreIndexLeftCounterEntry);
        if (counterEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        var counter = counterEntry.element();
        counterEntry.remove();
        if (isFiltering) {
            ElementAwareLinkedList<FilteringTracker<LeftTuple_>> leftTrackerList =
                    leftTuple.getStore(inputStoreIndexLeftTrackerList);
            leftTrackerList.clear(FilteringTracker::removeByLeft);
        }
        killCounterLeft(counter);
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightEntry) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(rightTuple));
        }
        rightTuple.setStore(inputStoreIndexRightEntry, rightTupleList.add(rightTuple));
        if (!isFiltering) {
            counterList.forEach(this::incrementCounterRight);
        } else {
            var rightTrackerList = new ElementAwareLinkedList<FilteringTracker<LeftTuple_>>();
            for (var counter : counterList) {
                updateCounterFromRight(counter, rightTuple, rightTrackerList);
            }
            rightTuple.setStore(inputStoreIndexRightTrackerList, rightTrackerList);
        }
    }

    @Override
    public final void updateRight(UniTuple<Right_> rightTuple) {
        ElementAwareLinkedList.Entry<UniTuple<Right_>> rightEntry = rightTuple.getStore(inputStoreIndexRightEntry);
        if (rightEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertRight(rightTuple);
            return;
        }
        if (isFiltering) {
            var rightTrackerList = clearRightTrackerList(rightTuple);
            for (var counter : counterList) {
                updateCounterFromRight(counter, rightTuple, rightTrackerList);
            }
        }
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        ElementAwareLinkedList.Entry<UniTuple<Right_>> rightEntry = rightTuple.removeStore(inputStoreIndexRightEntry);
        if (rightEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        rightEntry.remove();
        if (!isFiltering) {
            counterList.forEach(this::decrementCounterRight);
        } else {
            clearRightTrackerList(rightTuple);
        }
    }

}
