package ai.timefold.solver.core.impl.bavet.common;

import java.util.function.Consumer;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleStoreSizeTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.ElementAwareList;
import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

/**
 * This class has two direct children: {@link AbstractIndexedJoinNode} and {@link AbstractUnindexedJoinNode}.
 * The logic in either is identical, except that the latter removes all indexing work.
 * Therefore any time that one of the classes changes,
 * the other should be inspected if it could benefit from applying the change there too.
 *
 * @param <LeftTuple_>
 * @param <Right_>
 */
public abstract class AbstractJoinNode<LeftTuple_ extends AbstractTuple, Right_, OutTuple_ extends AbstractTuple>
        extends AbstractTwoInputNode<LeftTuple_, UniTuple<Right_>> {

    protected final int inputStoreIndexLeftOutTupleList;
    protected final int inputStoreIndexRightOutTupleList;
    private final boolean isFiltering;
    private final int outputStoreIndexLeftOutEntry;
    private final int outputStoreIndexRightOutEntry;
    protected final TupleStoreSizeTracker outputStoreSizeTracker;
    private final StaticPropagationQueue<OutTuple_> propagationQueue;

    protected AbstractJoinNode(TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, boolean isFiltering,
            TupleStorePositionTracker tupleStorePositionTracker) {
        this.inputStoreIndexLeftOutTupleList = tupleStorePositionTracker.reserveNextLeft();
        this.inputStoreIndexRightOutTupleList = tupleStorePositionTracker.reserveNextRight();
        this.isFiltering = isFiltering;
        this.outputStoreIndexLeftOutEntry = tupleStorePositionTracker.reserveNextOut();
        this.outputStoreIndexRightOutEntry = tupleStorePositionTracker.reserveNextOut();
        this.outputStoreSizeTracker = tupleStorePositionTracker;
        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
    }

    protected abstract OutTuple_ createOutTuple(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple);

    protected abstract void setOutTupleLeftFacts(OutTuple_ outTuple, LeftTuple_ leftTuple);

    protected abstract void setOutTupleRightFact(OutTuple_ outTuple, UniTuple<Right_> rightTuple);

    protected abstract boolean testFiltering(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple);

    protected final void insertOutTuple(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        var outTuple = createOutTuple(leftTuple, rightTuple);
        ElementAwareList<OutTuple_> outTupleListLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
        var outEntryLeft = outTupleListLeft.add(outTuple);
        outTuple.setStore(outputStoreIndexLeftOutEntry, outEntryLeft);
        ElementAwareList<OutTuple_> outTupleListRight = rightTuple.getStore(inputStoreIndexRightOutTupleList);
        var outEntryRight = outTupleListRight.add(outTuple);
        outTuple.setStore(outputStoreIndexRightOutEntry, outEntryRight);
        propagationQueue.insert(outTuple);
    }

    protected final void insertOutTupleFilteredFromLeft(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
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
        insertOutTupleFiltered(leftTuple, rightTuple);
    }

    protected final void insertOutTupleFiltered(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        if (!isFiltering || testFiltering(leftTuple, rightTuple)) {
            insertOutTuple(leftTuple, rightTuple);
        }
    }

    protected final void innerUpdateLeft(LeftTuple_ leftTuple, Consumer<Consumer<UniTuple<Right_>>> rightTupleConsumer) {
        // Prefer an update over retract-insert if possible
        ElementAwareList<OutTuple_> outTupleListLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
        // Propagate the update for downstream filters, matchWeighers, ...
        if (!isFiltering) {
            for (var outTuple : outTupleListLeft) {
                updateOutTupleLeft(outTuple, leftTuple);
            }
        } else {
            if (!leftTuple.state.isActive()) {
                // Assume the following scenario:
                // - The join is of two entities of the same type, both filtering out unassigned.
                // - One entity became unassigned, so the outTuple is getting retracted.
                // - The other entity is still assigned and is being updated.
                //
                // This means the filter would be called with (unassignedEntity, assignedEntity),
                // which breaks the expectation that the filter is only called on two assigned entities
                // and requires adding null checks to the filter for something that should intuitively be impossible.
                // We avoid this situation as it is clear that the outTuple must be retracted anyway,
                // and therefore any further updates to it are pointless.
                //
                // It is possible that the same problem would exist coming from the other side as well,
                // and therefore the right tuple would have to be checked for active state as well.
                // However, no such issue could have been reproduced; when in doubt, leave it out.
                return;
            }
            rightTupleConsumer.accept(rightTuple -> processOutTupleUpdate(leftTuple, rightTuple,
                    rightTuple.getStore(inputStoreIndexRightOutTupleList), outTupleListLeft, outputStoreIndexRightOutEntry));
        }
    }

    private void updateOutTupleLeft(OutTuple_ outTuple, LeftTuple_ leftTuple) {
        setOutTupleLeftFacts(outTuple, leftTuple);
        doUpdateOutTuple(outTuple);
    }

    private void doUpdateOutTuple(OutTuple_ outTuple) {
        var state = outTuple.state;
        if (!state.isActive()) { // Impossible because they shouldn't linger in the indexes.
            throw new IllegalStateException("Impossible state: The tuple (%s) in node (%s) is in an unexpected state (%s)."
                    .formatted(outTuple, this, outTuple.state));
        } else if (state != TupleState.OK) { // Already in the queue in the correct state.
            return;
        }
        propagationQueue.update(outTuple);
    }

    protected final void innerUpdateRight(UniTuple<Right_> rightTuple, Consumer<Consumer<LeftTuple_>> leftTupleConsumer) {
        // Prefer an update over retract-insert if possible
        ElementAwareList<OutTuple_> outTupleListRight = rightTuple.getStore(inputStoreIndexRightOutTupleList);
        if (!isFiltering) {
            // Propagate the update for downstream filters, matchWeighers, ...
            for (var outTuple : outTupleListRight) {
                setOutTupleRightFact(outTuple, rightTuple);
                doUpdateOutTuple(outTuple);
            }
        } else {
            leftTupleConsumer.accept(leftTuple -> processOutTupleUpdateFromLeft(leftTuple, rightTuple,
                    leftTuple.getStore(inputStoreIndexLeftOutTupleList), outTupleListRight, outputStoreIndexLeftOutEntry));
        }
    }

    private void processOutTupleUpdateFromLeft(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple,
            ElementAwareList<OutTuple_> outList, ElementAwareList<OutTuple_> outTupleList, int outputStoreIndexOutEntry) {
        if (!leftTuple.state.isActive()) {
            // Assume the following scenario:
            // - The join is of two entities of the same type, both filtering out unassigned.
            // - One entity became unassigned, so the outTuple is getting retracted.
            // - The other entity is still assigned and is being updated.
            //
            // This means the filter would be called with (unassignedEntity, assignedEntity),
            // which breaks the expectation that the filter is only called on two assigned entities
            // and requires adding null checks to the filter for something that should intuitively be impossible.
            // We avoid this situation as it is clear that the outTuple must be retracted anyway,
            // and therefore any further updates to it are pointless.
            //
            // It is possible that the same problem would exist coming from the other side as well,
            // and therefore the right tuple would have to be checked for active state as well.
            // However, no such issue could have been reproduced; when in doubt, leave it out.
            return;
        }
        processOutTupleUpdate(leftTuple, rightTuple, outList, outTupleList, outputStoreIndexOutEntry);
    }

    private void processOutTupleUpdate(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple, ElementAwareList<OutTuple_> outList,
            ElementAwareList<OutTuple_> outTupleList, int outputStoreIndexOutEntry) {
        var outTuple = findOutTuple(outTupleList, outList, outputStoreIndexOutEntry);
        if (testFiltering(leftTuple, rightTuple)) {
            if (outTuple == null) {
                insertOutTuple(leftTuple, rightTuple);
            } else {
                updateOutTupleLeft(outTuple, leftTuple);
            }
        } else {
            if (outTuple != null) {
                retractOutTuple(outTuple);
            }
        }
    }

    private static <Tuple_ extends AbstractTuple> Tuple_ findOutTuple(ElementAwareList<Tuple_> outTupleList,
            ElementAwareList<Tuple_> outList, int outputStoreIndexOutEntry) {
        // Hack: the outTuple has no left/right input tuple reference, use the left/right outList reference instead.
        var item = outTupleList.first();
        while (item != null) {
            // Creating list iterators here caused major GC pressure; therefore, we iterate over the entries directly.
            var outTuple = item.getElement();
            ElementAwareListEntry<Tuple_> outEntry = outTuple.getStore(outputStoreIndexOutEntry);
            var outEntryList = outEntry.getList();
            if (outList == outEntryList) {
                return outTuple;
            }
            item = item.next();
        }
        return null;
    }

    private void retractOutTuple(OutTuple_ outTuple) {
        removeLeftEntry(outTuple);
        removeRightEntry(outTuple);
        propagateRetract(outTuple);
    }

    private void removeLeftEntry(OutTuple_ outTuple) {
        removeEntry(outTuple, outputStoreIndexLeftOutEntry);
    }

    private void removeRightEntry(OutTuple_ outTuple) {
        removeEntry(outTuple, outputStoreIndexRightOutEntry);
    }

    private void removeEntry(OutTuple_ outTuple, int outputStoreIndex) {
        ElementAwareListEntry<OutTuple_> outEntry = outTuple.removeStore(outputStoreIndex);
        outEntry.remove();
    }

    private void propagateRetract(OutTuple_ outTuple) {
        var state = outTuple.state;
        if (!state.isActive()) { // Impossible because they shouldn't linger in the indexes.
            throw new IllegalStateException("Impossible state: The tuple (%s) in node (%s) is in an unexpected state (%s)."
                    .formatted(outTuple, this, outTuple.state));
        }
        propagationQueue.retract(outTuple, state == TupleState.CREATING ? TupleState.ABORTING : TupleState.DYING);
    }

    void retractOutTupleByLeft(OutTuple_ outTuple) {
        outTuple.removeStore(outputStoreIndexLeftOutEntry); // The caller will clear the entire list in one go.
        removeRightEntry(outTuple);
        propagateRetract(outTuple);
    }

    void retractOutTupleByRight(OutTuple_ outTuple) {
        removeLeftEntry(outTuple);
        outTuple.removeStore(outputStoreIndexRightOutEntry); // The caller will clear the entire list in one go.
        propagateRetract(outTuple);
    }

    @Override
    public Propagator getPropagator() {
        return propagationQueue;
    }

}
