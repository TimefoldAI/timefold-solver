package ai.timefold.solver.core.impl.score.stream.bavet.common;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.ElementAwareList;

/**
 * There is a strong likelihood that any change made to this class
 * should also be made to {@link AbstractIndexedIfExistsNode}.
 *
 * @param <LeftTuple_>
 * @param <Right_>
 */
public abstract class AbstractUnindexedIfExistsNode<LeftTuple_ extends AbstractTuple, Right_>
        extends AbstractIfExistsNode<LeftTuple_, Right_>
        implements LeftTupleLifecycle<LeftTuple_>, RightTupleLifecycle<UniTuple<Right_>> {

    private final int inputStoreIndexLeftCounterEntry;

    private final int inputStoreIndexRightEntry;

    // Acts as a leftTupleList too
    private final ElementAwareList<ExistsCounter<LeftTuple_>> leftCounterList = new ElementAwareList<>();
    private final ElementAwareList<UniTuple<Right_>> rightTupleList = new ElementAwareList<>();

    protected AbstractUnindexedIfExistsNode(boolean shouldExist,
            int inputStoreIndexLeftCounterEntry, int inputStoreIndexLeftTrackerList, int inputStoreIndexRightEntry,
            int inputStoreIndexRightTrackerList,
            TupleLifecycle<LeftTuple_> nextNodesTupleLifecycle, boolean isFiltering) {
        super(shouldExist,
                inputStoreIndexLeftTrackerList, inputStoreIndexRightTrackerList,
                nextNodesTupleLifecycle, isFiltering);
        this.inputStoreIndexLeftCounterEntry = inputStoreIndexLeftCounterEntry;
        this.inputStoreIndexRightEntry = inputStoreIndexRightEntry;
    }

    @Override
    public final void insertLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftCounterEntry) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + leftTuple
                    + ") was already added in the tupleStore.");
        }
        ExistsCounter<LeftTuple_> counter = new ExistsCounter<>(leftTuple);
        ElementAwareList<ExistsCounter<LeftTuple_>>.Entry counterEntry = leftCounterList.add(counter);
        leftTuple.setStore(inputStoreIndexLeftCounterEntry, counterEntry);

        if (!isFiltering) {
            counter.countRight = rightTupleList.size();
        } else {
            ElementAwareList<FilteringTracker<LeftTuple_>> leftTrackerList = new ElementAwareList<>();
            rightTupleList.forEach(leftTuple, counter, leftTrackerList, this::updateCounterFromLeft);
            leftTuple.setStore(inputStoreIndexLeftTrackerList, leftTrackerList);
        }
        initCounterLeft(counter);
    }

    @Override
    public final void updateLeft(LeftTuple_ leftTuple) {
        ElementAwareList<ExistsCounter<LeftTuple_>>.Entry counterEntry = leftTuple.getStore(inputStoreIndexLeftCounterEntry);
        if (counterEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertLeft(leftTuple);
            return;
        }
        ExistsCounter<LeftTuple_> counter = counterEntry.getElement();
        // The indexers contain counters in the DEAD state, to track the rightCount.
        if (!isFiltering) {
            updateUnchangedCounterLeft(counter);
        } else {
            // Call filtering for the leftTuple and rightTuple combinations again
            ElementAwareList<FilteringTracker<LeftTuple_>> leftTrackerList = leftTuple.getStore(inputStoreIndexLeftTrackerList);
            leftTrackerList.forEach(FilteringTracker::remove);
            counter.countRight = 0;
            rightTupleList.forEach(leftTuple, counter, leftTrackerList, this::updateCounterFromLeft);
            updateCounterLeft(counter);
        }
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        ElementAwareList<ExistsCounter<LeftTuple_>>.Entry counterEntry = leftTuple.removeStore(inputStoreIndexLeftCounterEntry);
        if (counterEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ExistsCounter<LeftTuple_> counter = counterEntry.getElement();
        counterEntry.remove();
        if (isFiltering) {
            ElementAwareList<FilteringTracker<LeftTuple_>> leftTrackerList = leftTuple.getStore(inputStoreIndexLeftTrackerList);
            leftTrackerList.forEach(FilteringTracker::remove);
        }
        killCounterLeft(counter);
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightEntry) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + rightTuple
                    + ") was already added in the tupleStore.");
        }
        ElementAwareList<UniTuple<Right_>>.Entry rightEntry = rightTupleList.add(rightTuple);
        rightTuple.setStore(inputStoreIndexRightEntry, rightEntry);
        if (!isFiltering) {
            leftCounterList.forEach(this::incrementCounterRight);
        } else {
            ElementAwareList<FilteringTracker<LeftTuple_>> rightTrackerList = new ElementAwareList<>();
            leftCounterList.forEach(rightTuple, rightTrackerList, this::updateCounterFromRight);
            rightTuple.setStore(inputStoreIndexRightTrackerList, rightTrackerList);
        }
    }

    @Override
    public final void updateRight(UniTuple<Right_> rightTuple) {
        ElementAwareList<UniTuple<Right_>>.Entry rightEntry = rightTuple.getStore(inputStoreIndexRightEntry);
        if (rightEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertRight(rightTuple);
            return;
        }
        if (isFiltering) {
            ElementAwareList<FilteringTracker<LeftTuple_>> rightTrackerList = updateRightTrackerList(rightTuple);
            leftCounterList.forEach(rightTuple, rightTrackerList, this::updateCounterFromRight);
        }
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        ElementAwareList<UniTuple<Right_>>.Entry rightEntry = rightTuple.removeStore(inputStoreIndexRightEntry);
        if (rightEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        rightEntry.remove();
        if (!isFiltering) {
            leftCounterList.forEach(this::decrementCounterRight);
        } else {
            updateRightTrackerList(rightTuple);
        }
    }

}
