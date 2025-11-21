package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.ElementAwareList;
import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

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

    private final int inputStoreIndexLeftEntry;
    private final int inputStoreIndexRightEntry;
    private final ElementAwareList<LeftTuple_> leftTupleList = new ElementAwareList<>();
    private final ElementAwareList<UniTuple<Right_>> rightTupleList = new ElementAwareList<>();

    protected AbstractUnindexedJoinNode(TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, boolean isFiltering,
            TupleStorePositionTracker tupleStorePositionTracker) {
        super(nextNodesTupleLifecycle, isFiltering, tupleStorePositionTracker);
        this.inputStoreIndexLeftEntry = tupleStorePositionTracker.reserveNextLeft();
        this.inputStoreIndexRightEntry = tupleStorePositionTracker.reserveNextRight();
    }

    @Override
    public final void insertLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftEntry) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + leftTuple
                    + ") was already added in the tupleStore.");
        }
        var leftEntry = leftTupleList.add(leftTuple);
        leftTuple.setStore(inputStoreIndexLeftEntry, leftEntry);
        var outTupleListLeft = new ElementAwareList<OutTuple_>();
        leftTuple.setStore(inputStoreIndexLeftOutTupleList, outTupleListLeft);
        if (!leftTuple.state.isActive()) {
            // Assume the following scenario:
            // - The join is of two entities of the same type, both filtering out unassigned.
            // - One entity became unassigned, so the outTuple is getting retracted.
            // - The other entity became assigned, as is therefore getting inserted.
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
        for (var tuple : rightTupleList) {
            insertOutTupleFiltered(leftTuple, tuple);
        }
    }

    @Override
    public final void updateLeft(LeftTuple_ leftTuple) {
        ElementAwareListEntry<LeftTuple_> leftEntry = leftTuple.getStore(inputStoreIndexLeftEntry);
        if (leftEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertLeft(leftTuple);
            return;
        }
        innerUpdateLeft(leftTuple, rightTupleList::forEach);
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        ElementAwareListEntry<LeftTuple_> leftEntry = leftTuple.removeStore(inputStoreIndexLeftEntry);
        if (leftEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ElementAwareList<OutTuple_> outTupleListLeft = leftTuple.removeStore(inputStoreIndexLeftOutTupleList);
        leftEntry.remove();
        outTupleListLeft.clear(this::retractOutTupleByLeft);
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightEntry) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + rightTuple
                    + ") was already added in the tupleStore.");
        }
        var rightEntry = rightTupleList.add(rightTuple);
        rightTuple.setStore(inputStoreIndexRightEntry, rightEntry);
        var outTupleListRight = new ElementAwareList<OutTuple_>();
        rightTuple.setStore(inputStoreIndexRightOutTupleList, outTupleListRight);
        for (var tuple : leftTupleList) {
            insertOutTupleFilteredFromLeft(tuple, rightTuple);
        }
    }

    @Override
    public final void updateRight(UniTuple<Right_> rightTuple) {
        ElementAwareListEntry<UniTuple<Right_>> rightEntry = rightTuple.getStore(inputStoreIndexRightEntry);
        if (rightEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertRight(rightTuple);
            return;
        }
        innerUpdateRight(rightTuple, leftTupleList::forEach);
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        ElementAwareListEntry<UniTuple<Right_>> rightEntry = rightTuple.removeStore(inputStoreIndexRightEntry);
        if (rightEntry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ElementAwareList<OutTuple_> outTupleListRight = rightTuple.removeStore(inputStoreIndexRightOutTupleList);
        rightEntry.remove();
        outTupleListRight.clear(this::retractOutTupleByRight);
    }

}
