package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.InTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.indictment.IndictmentSource;
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
            // Trackers link themselves into the left tuple's inputStoreIndexLeftTrackerList slot.
            // No list object is needed; the slot starts null and the first tracker becomes the head.
            for (var rightTuple : rightTupleList) {
                updateCounterFromLeft(counter, rightTuple);
            }
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
            clearLeftTrackerList(leftTuple);
            counter.countRight = 0;
            for (var rightTuple : rightTupleList) {
                updateCounterFromLeft(counter, rightTuple);
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
        clearLeftTrackerList(leftTuple);
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
            // To prevent creating a dynamic lambda on the hot path,
            // only call the 2-args version when indictments are enabled
            if (rightTuple.getIndictmentSource() == IndictmentSource.DISABLED) {
                counterList.forEach(this::incrementCounterRightWithoutIndictment);
            } else {
                counterList.forEach(counter -> incrementCounterRightUpdatingIndictment(counter, rightTuple));
            }
        } else {
            // Trackers link themselves into the right tuple's inputStoreIndexRightTrackerList slot.
            // No list object is needed; the slot starts null and the first tracker becomes the head.
            for (var counter : counterList) {
                updateCounterFromRight(counter, rightTuple);
            }
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
            clearRightTrackerList(rightTuple);
            for (var counter : counterList) {
                updateCounterFromRight(counter, rightTuple);
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
            // To prevent creating a dynamic lambda on the hot path,
            // only call the 2-args version when indictments are enabled
            if (rightTuple.getIndictmentSource() == IndictmentSource.DISABLED) {
                counterList.forEach(this::decrementCounterRightWithoutIndictment);
            } else {
                counterList.forEach(counter -> decrementCounterRightUpdatingIndictment(counter, rightTuple));
            }
        } else {
            clearRightTrackerList(rightTuple);
        }
    }

}
