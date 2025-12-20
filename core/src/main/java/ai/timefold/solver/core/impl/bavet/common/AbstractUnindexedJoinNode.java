package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.InOutTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.ElementAwareLinkedList;

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

    private final int inputStoreIndexLeftEntry;
    private final int inputStoreIndexRightEntry;
    private final ElementAwareLinkedList<LeftTuple_> leftTupleList = new ElementAwareLinkedList<>();
    private final ElementAwareLinkedList<UniTuple<Right_>> rightTupleList = new ElementAwareLinkedList<>();

    protected AbstractUnindexedJoinNode(TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, boolean isFiltering,
            InOutTupleStorePositionTracker tupleStorePositionTracker) {
        super(nextNodesTupleLifecycle, isFiltering, tupleStorePositionTracker);
        this.inputStoreIndexLeftEntry = tupleStorePositionTracker.reserveNextLeft();
        this.inputStoreIndexRightEntry = tupleStorePositionTracker.reserveNextRight();
    }

    @Override
    public final void insertLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftEntry) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(leftTuple));
        }
        leftTuple.setStore(inputStoreIndexLeftEntry, leftTupleList.add(leftTuple));
        leftTuple.setStore(inputStoreIndexLeftOutTupleList, new ElementAwareLinkedList<OutTuple_>());
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
        for (var rightTuple : rightTupleList) {
            insertOutTupleFiltered(leftTuple, rightTuple);
        }
    }

    @Override
    public final void updateLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftEntry) == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertLeft(leftTuple);
            return;
        }
        innerUpdateLeft(leftTuple, rightTupleList::forEach);
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        ElementAwareLinkedList.Entry<LeftTuple_> leftEntry = leftTuple.removeStore(inputStoreIndexLeftEntry);
        if (leftEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ElementAwareLinkedList<OutTuple_> outTupleListLeft = leftTuple.removeStore(inputStoreIndexLeftOutTupleList);
        leftEntry.remove();
        outTupleListLeft.clear(this::retractOutTupleByLeft);
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightEntry) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(rightTuple));
        }
        rightTuple.setStore(inputStoreIndexRightEntry, rightTupleList.add(rightTuple));
        rightTuple.setStore(inputStoreIndexRightOutTupleList, new ElementAwareLinkedList<OutTuple_>());
        for (var leftTuple : leftTupleList) {
            insertOutTupleFilteredFromLeft(leftTuple, rightTuple);
        }
    }

    @Override
    public final void updateRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightEntry) == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertRight(rightTuple);
            return;
        }
        innerUpdateRight(rightTuple, leftTupleList::forEach);
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        ElementAwareLinkedList.Entry<UniTuple<Right_>> rightEntry = rightTuple.removeStore(inputStoreIndexRightEntry);
        if (rightEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ElementAwareLinkedList<OutTuple_> outTupleListRight = rightTuple.removeStore(inputStoreIndexRightOutTupleList);
        rightEntry.remove();
        outTupleListRight.clear(this::retractOutTupleByRight);
    }

}
