package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.InOutTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleList;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

/**
 * There is a strong likelihood that any change made to this class
 * should also be made to {@link AbstractIndexedJoinNode}.
 *
 * @param <LeftTuple_>
 * @param <Right_>
 */
public abstract class AbstractUnindexedJoinNode<LeftTuple_ extends Tuple, Right_, OutTuple_ extends Tuple>
        extends AbstractJoinNode<LeftTuple_, Right_, OutTuple_>
        implements LeftTupleLifecycle<LeftTuple_>, RightTupleLifecycle<UniTuple<Right_>> {

    private final TupleList<LeftTuple_> leftTupleList;
    private final TupleList<UniTuple<Right_>> rightTupleList;

    protected AbstractUnindexedJoinNode(TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, boolean isFiltering,
            InOutTupleStorePositionTracker tupleStorePositionTracker) {
        super(nextNodesTupleLifecycle, isFiltering, tupleStorePositionTracker);
        this.leftTupleList = new TupleList<>(tupleStorePositionTracker.reserveNextLeft(),
                tupleStorePositionTracker.reserveNextLeft());
        this.rightTupleList = new TupleList<>(tupleStorePositionTracker.reserveNextRight(),
                tupleStorePositionTracker.reserveNextRight());
    }

    @Override
    public final void insertLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftOutTupleList) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(leftTuple));
        }
        leftTupleList.add(leftTuple);
        leftTuple.setStore(inputStoreIndexLeftOutTupleList, leftOutTupleListBuilder.get());
        if (!leftTuple.getState().isActive()) {
            // Assume the following scenario:
            // - The join is of two entities of the same type, both filtering out unassigned.
            // - One entity became unassigned, so the outTuple is getting retracted.
            // - The other entity became assigned, and is therefore getting inserted.
            //
            // This means the filter would be called with (unassignedEntity, assignedEntity),
            // which breaks the expectation that the filter is only called on two assigned entities
            // and requires adding null checks to the filter for something that should intuitively be impossible.
            // We avoid this situation as it is clear that it is pointless to insert this tuple.
            //
            // It is possible that the same problem would exist coming from the other side as well,
            // and therefore the right tuple would have to be checked for active state as well.
            // However, no such issue could have been reproduced; when in doubt, leave it out.
            return;
        }
        for (var rightTuple = rightTupleList.first(); rightTuple != null; rightTuple = rightTupleList.next(rightTuple)) {
            insertOutTupleFiltered(leftTuple, rightTuple);
        }
    }

    @Override
    public final void updateLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftOutTupleList) == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertLeft(leftTuple);
            return;
        }
        innerUpdateLeft(leftTuple, rightTupleList::forEach);
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        TupleList<OutTuple_> outTupleListLeft = leftTuple.removeStore(inputStoreIndexLeftOutTupleList);
        if (outTupleListLeft == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        leftTupleList.remove(leftTuple);
        outTupleListLeft.clear(this::retractOutTupleByLeft);
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightOutTupleList) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(rightTuple));
        }
        rightTupleList.add(rightTuple);
        rightTuple.setStore(inputStoreIndexRightOutTupleList, rightOutTupleListBuilder.get());
        for (var leftTuple = leftTupleList.first(); leftTuple != null; leftTuple = leftTupleList.next(leftTuple)) {
            insertOutTupleFilteredFromLeft(leftTuple, rightTuple);
        }
    }

    @Override
    public final void updateRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightOutTupleList) == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertRight(rightTuple);
            return;
        }
        innerUpdateRight(rightTuple, leftTupleList::forEach);
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        TupleList<OutTuple_> outTupleListRight = rightTuple.removeStore(inputStoreIndexRightOutTupleList);
        if (outTupleListRight == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        rightTupleList.remove(rightTuple);
        outTupleListRight.clear(this::retractOutTupleByRight);
    }

}
