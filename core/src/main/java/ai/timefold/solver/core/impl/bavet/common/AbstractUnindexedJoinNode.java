package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.index.IndexedSet;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.OutputStoreSizeTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

/**
 * There is a strong likelihood that any change made to this class
 * should also be made to {@link AbstractIndexedJoinNode}.
 *
 * @param <LeftTuple_>
 * @param <Right_>
 */
public abstract class AbstractUnindexedJoinNode<LeftTuple_ extends AbstractTuple, Right_, OutTuple_ extends AbstractTuple>
        extends AbstractJoinNode<LeftTuple_, Right_, OutTuple_>
        implements LeftTupleLifecycle<LeftTuple_>, RightTupleLifecycle<UniTuple<Right_>> {

    private final int inputStoreIndexLeftPosition;
    private final int inputStoreIndexRightPosition;
    private final int outputStoreIndexLeftPosition;
    private final int outputStoreIndexRightPosition;
    private final IndexedSet<LeftTuple_> leftTupleSet;
    private final IndexedSet<UniTuple<Right_>> rightTupleSet;

    protected AbstractUnindexedJoinNode(TupleStorePositionTracker leftTupleStorePositionTracker,
            TupleStorePositionTracker rightTupleStorePositionTracker, OutputStoreSizeTracker outputStoreSizeTracker,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, boolean isFiltering) {
        super(leftTupleStorePositionTracker, rightTupleStorePositionTracker, outputStoreSizeTracker, nextNodesTupleLifecycle,
                isFiltering);
        this.inputStoreIndexLeftPosition = leftTupleStorePositionTracker.reserveNextAvailablePosition();
        this.inputStoreIndexRightPosition = rightTupleStorePositionTracker.reserveNextAvailablePosition();
        this.outputStoreIndexLeftPosition = outputStoreSizeTracker.reserveNextAvailablePosition();
        this.outputStoreIndexRightPosition = outputStoreSizeTracker.reserveNextAvailablePosition();
        this.leftTupleSet = new IndexedSet<>(new TuplePositionTracker<>(inputStoreIndexLeftPosition));
        this.rightTupleSet = new IndexedSet<>(new TuplePositionTracker<>(inputStoreIndexRightPosition));
    }

    @Override
    public final void insertLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftPosition) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(leftTuple));
        }
        leftTupleSet.add(leftTuple);
        var outTupleSetLeft = new IndexedSet<>(new TuplePositionTracker<>(outputStoreIndexLeftPosition));
        leftTuple.setStore(inputStoreIndexLeftOutTupleSet, outTupleSetLeft);
        rightTupleSet.forEach(tuple -> insertOutTupleFiltered(leftTuple, tuple));
    }

    @Override
    public final void updateLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftPosition) == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertLeft(leftTuple);
            return;
        }
        innerUpdateLeft(leftTuple, rightTupleSet::forEach);
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftPosition) == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        IndexedSet<OutTuple_> outTupleSetLeft = leftTuple.removeStore(inputStoreIndexLeftOutTupleSet);
        leftTupleSet.remove(leftTuple);
        outTupleSetLeft.clear(this::retractOutTupleByLeft);
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightPosition) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(rightTuple));
        }
        rightTupleSet.add(rightTuple);
        var outTupleSetRight = new IndexedSet<OutTuple_>(new TuplePositionTracker<>(outputStoreIndexRightPosition));
        rightTuple.setStore(inputStoreIndexRightOutTupleSet, outTupleSetRight);
        leftTupleSet.forEach(tuple -> insertOutTupleFiltered(tuple, rightTuple));
    }

    @Override
    public final void updateRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightPosition) == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertRight(rightTuple);
            return;
        }
        innerUpdateRight(rightTuple, leftTupleSet::forEach);
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightPosition) == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        IndexedSet<OutTuple_> outTupleSetRight = rightTuple.removeStore(inputStoreIndexRightOutTupleSet);
        rightTupleSet.remove(rightTuple);
        outTupleSetRight.clear(this::retractOutTupleByRight);
    }

}
