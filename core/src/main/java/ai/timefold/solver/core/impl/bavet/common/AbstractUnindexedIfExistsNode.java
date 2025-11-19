package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.index.IndexedSet;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

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

    private final int inputStoreIndexLeftCounter;
    private final int inputStoreIndexRightTuple;

    private final IndexedSet<ExistsCounter<LeftTuple_>> leftCounterSet;
    private final IndexedSet<UniTuple<Right_>> rightTupleSet;

    protected AbstractUnindexedIfExistsNode(boolean shouldExist, TupleStorePositionTracker leftTupleStorePositionTracker,
            TupleStorePositionTracker rightTupleStorePositionTracker, TupleLifecycle<LeftTuple_> nextNodesTupleLifecycle,
            boolean isFiltering) {
        super(shouldExist, rightTupleStorePositionTracker, nextNodesTupleLifecycle, isFiltering);
        this.inputStoreIndexLeftCounter = leftTupleStorePositionTracker.reserveNextAvailablePosition();
        this.inputStoreIndexRightTuple = rightTupleStorePositionTracker.reserveNextAvailablePosition();
        this.leftCounterSet = new IndexedSet<>(ExistsCounterPositionTracker.instance());
        this.rightTupleSet = new IndexedSet<>(new TuplePositionTracker<>(inputStoreIndexRightTuple));
    }

    @Override
    public final void insertLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftCounter) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(leftTuple));
        }
        var counter = new ExistsCounter<>(leftTuple);
        leftCounterSet.add(counter);
        leftTuple.setStore(inputStoreIndexLeftCounter, counter);

        if (!isFiltering) {
            counter.countRight = rightTupleSet.size();
        } else {
            rightTupleSet.forEach(tuple -> updateCounterFromLeft(counter, tuple));
        }
        initCounterLeft(counter);
    }

    @Override
    public final void updateLeft(LeftTuple_ leftTuple) {
        ExistsCounter<LeftTuple_> counter = leftTuple.getStore(inputStoreIndexLeftCounter);
        if (counter == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertLeft(leftTuple);
            return;
        }
        // The indexers contain counters in the DEAD state, to track the rightCount.
        if (!isFiltering) {
            updateUnchangedCounterLeft(counter);
        } else {
            // Call filtering for the leftTuple and rightTuple combinations again
            counter.clearIncludingCount();
            rightTupleSet.forEach(tuple -> updateCounterFromLeft(counter, tuple));
            updateCounterLeft(counter);
        }
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        ExistsCounter<LeftTuple_> counter = leftTuple.removeStore(inputStoreIndexLeftCounter);
        if (counter == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        leftCounterSet.remove(counter);
        if (isFiltering) {
            counter.clearWithoutCount();
        }
        killCounterLeft(counter);
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightTuple) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(rightTuple));
        }
        rightTupleSet.add(rightTuple);
        if (!isFiltering) {
            leftCounterSet.forEach(this::incrementCounterRight);
        } else {
            var rightHandleSet = new IndexedSet<ExistsCounterHandle<LeftTuple_>>(ExistsCounterHandlePositionTracker.right());
            leftCounterSet.forEach(counter -> updateCounterFromRight(counter, rightTuple, rightHandleSet));
            rightTuple.setStore(inputStoreIndexRightHandleSet, rightHandleSet);
        }
    }

    @Override
    public final void updateRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightTuple) == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertRight(rightTuple);
            return;
        }
        if (isFiltering) {
            var rightHandleSet = updateRightHandleSet(rightTuple);
            leftCounterSet.forEach(counter -> updateCounterFromRight(counter, rightTuple, rightHandleSet));
        }
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightTuple) == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        rightTupleSet.remove(rightTuple);
        if (!isFiltering) {
            leftCounterSet.forEach(this::decrementCounterRight);
        } else {
            updateRightHandleSet(rightTuple);
        }
    }

}
