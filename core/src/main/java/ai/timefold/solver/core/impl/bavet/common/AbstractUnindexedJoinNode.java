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
        if (isFiltering) {
            // Defer the cross-match (the opposite-side read) to this node's own layer turn instead of
            // computing it now, at whatever layer the parent that produced leftTuple happens to be in.
            // See AbstractJoinNode's pendingLeft/pendingRight javadoc.
            enqueuePendingLeft(leftTuple);
            return;
        }
        if (!leftTuple.isActiveTransitively()) {
            // See Tuple#isActiveTransitively for why the immediate state alone isn't enough here.
            return;
        }
        // The right tuples come out of the list and can be retracting for the mirror-image reason,
        // hence insertOutTupleFilteredFromRight(...) rather than the unguarded insert.
        for (var rightTuple = rightTupleList.first(); rightTuple != null; rightTuple = rightTupleList.next(rightTuple)) {
            insertOutTupleFilteredRight(leftTuple, rightTuple);
        }
    }

    @Override
    public final void updateLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftOutTupleList) == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertLeft(leftTuple);
            return;
        }
        if (isFiltering) {
            enqueuePendingLeft(leftTuple);
        } else {
            innerUpdateLeft(leftTuple, rightTupleList::forEach);
        }
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        clearPendingLeft(leftTuple);
        TupleList<OutTuple_> outTupleListLeft = leftTuple.removeStore(inputStoreIndexLeftOutTupleList);
        if (outTupleListLeft == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        leftTupleList.remove(leftTuple);
        outTupleListLeft.clear(this::retractOutTupleLeft);
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
        if (isFiltering) {
            // See the mirror comment in insertLeft.
            enqueuePendingRight(rightTuple);
            return;
        }
        for (var leftTuple = leftTupleList.first(); leftTuple != null; leftTuple = leftTupleList.next(leftTuple)) {
            insertOutTupleFilteredLeft(leftTuple, rightTuple);
        }
    }

    @Override
    public final void updateRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightOutTupleList) == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertRight(rightTuple);
            return;
        }
        if (isFiltering) {
            enqueuePendingRight(rightTuple);
        } else {
            innerUpdateRight(rightTuple, leftTupleList::forEach);
        }
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        clearPendingRight(rightTuple);
        TupleList<OutTuple_> outTupleListRight = rightTuple.removeStore(inputStoreIndexRightOutTupleList);
        if (outTupleListRight == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        rightTupleList.remove(rightTuple);
        outTupleListRight.clear(this::retractOutTupleRight);
    }

    @Override
    protected void reconcilePendingLeft(LeftTuple_ leftTuple) {
        innerUpdateLeft(leftTuple, rightTupleList::forEach);
    }

    @Override
    protected void reconcilePendingRight(UniTuple<Right_> rightTuple) {
        innerUpdateRight(rightTuple, leftTupleList::forEach);
    }

}
