package ai.timefold.solver.core.impl.bavet.common;

import java.util.function.Consumer;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.bavet.common.tuple.InOutTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.OutTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleList;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

/**
 * This class has two direct children: {@link AbstractIndexedJoinNode} and {@link AbstractUnindexedJoinNode}.
 * The logic in either is identical, except that the latter removes all indexing work.
 * Therefore any time that one of the classes changes,
 * the other should be inspected if it could benefit from applying the change there too.
 *
 * @param <LeftTuple_>
 * @param <Right_>
 */
public abstract class AbstractJoinNode<LeftTuple_ extends Tuple, Right_, OutTuple_ extends Tuple>
        extends AbstractTwoInputNode<LeftTuple_, UniTuple<Right_>> {

    protected final int inputStoreIndexLeftOutTupleList;
    protected final int inputStoreIndexRightOutTupleList;
    private final boolean isFiltering;
    private final int outputStoreIndexLeftOutTupleList;
    private final int outputStoreIndexRightOutTupleList;
    protected final OutTupleStorePositionTracker outputStoreSizeTracker;
    private final StaticPropagationQueue<OutTuple_> propagationQueue;

    protected final Supplier<TupleList<OutTuple_>> leftOutTupleListBuilder;
    protected final Supplier<TupleList<OutTuple_>> rightOutTupleListBuilder;

    protected AbstractJoinNode(TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, boolean isFiltering,
            InOutTupleStorePositionTracker tupleStorePositionTracker) {
        super(nextNodesTupleLifecycle);
        this.inputStoreIndexLeftOutTupleList = tupleStorePositionTracker.reserveNextLeft();
        this.inputStoreIndexRightOutTupleList = tupleStorePositionTracker.reserveNextRight();
        this.isFiltering = isFiltering;
        this.outputStoreIndexLeftOutTupleList = tupleStorePositionTracker.reserveNextOut();
        this.outputStoreIndexRightOutTupleList = tupleStorePositionTracker.reserveNextOut();
        this.outputStoreSizeTracker = tupleStorePositionTracker;
        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);

        var outputStoreIndexLeftOutPrev = tupleStorePositionTracker.reserveNextOut();
        var outputStoreIndexLeftOutNext = tupleStorePositionTracker.reserveNextOut();
        var outputStoreIndexRightOutPrev = tupleStorePositionTracker.reserveNextOut();
        var outputStoreIndexRightOutNext = tupleStorePositionTracker.reserveNextOut();
        this.leftOutTupleListBuilder = () -> new TupleList<>(outputStoreIndexLeftOutPrev, outputStoreIndexLeftOutNext);
        this.rightOutTupleListBuilder = () -> new TupleList<>(outputStoreIndexRightOutPrev, outputStoreIndexRightOutNext);
    }

    @Override
    public StreamKind getStreamKind() {
        return StreamKind.JOIN;
    }

    protected abstract OutTuple_ createOutTuple(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple);

    protected abstract void setOutTupleLeftFacts(OutTuple_ outTuple, LeftTuple_ leftTuple);

    protected abstract void setOutTupleRightFact(OutTuple_ outTuple, UniTuple<Right_> rightTuple);

    protected abstract boolean testFiltering(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple);

    protected final void insertOutTuple(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        var outTuple = createOutTuple(leftTuple, rightTuple);
        TupleList<OutTuple_> outTupleListLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
        outTupleListLeft.add(outTuple);
        outTuple.setStore(outputStoreIndexLeftOutTupleList, outTupleListLeft);
        TupleList<OutTuple_> outTupleListRight = rightTuple.getStore(inputStoreIndexRightOutTupleList);
        outTupleListRight.add(outTuple);
        outTuple.setStore(outputStoreIndexRightOutTupleList, outTupleListRight);
        propagationQueue.insert(outTuple);
    }

    protected final void insertOutTupleFilteredFromLeft(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
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
        insertOutTupleFiltered(leftTuple, rightTuple);
    }

    protected final void insertOutTupleFiltered(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        if (!isFiltering || testFiltering(leftTuple, rightTuple)) {
            insertOutTuple(leftTuple, rightTuple);
        }
    }

    protected final void innerUpdateLeft(LeftTuple_ leftTuple, Consumer<Consumer<UniTuple<Right_>>> rightTupleConsumer) {
        // Prefer an update over retract-insert if possible
        TupleList<OutTuple_> outTupleListLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
        // Propagate the update for downstream filters, matchWeighers, ...
        if (!isFiltering) {
            for (var outTuple = outTupleListLeft.first(); outTuple != null; outTuple = outTupleListLeft.next(outTuple)) {
                updateOutTupleLeft(outTuple, leftTuple);
            }
        } else {
            if (!leftTuple.getState().isActive()) {
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
                    rightTuple.getStore(inputStoreIndexRightOutTupleList), outTupleListLeft,
                    outputStoreIndexRightOutTupleList));
        }
    }

    private void updateOutTupleLeft(OutTuple_ outTuple, LeftTuple_ leftTuple) {
        setOutTupleLeftFacts(outTuple, leftTuple);
        doUpdateOutTuple(outTuple);
    }

    private void doUpdateOutTuple(OutTuple_ outTuple) {
        var state = outTuple.getState();
        if (!state.isActive()) { // Impossible because they shouldn't linger in the indexes.
            throw new IllegalStateException("Impossible state: The tuple (%s) in node (%s) is in an unexpected state (%s)."
                    .formatted(outTuple, this, state));
        } else if (state != TupleState.OK) { // Already in the queue in the correct state.
            return;
        }
        propagationQueue.update(outTuple);
    }

    protected final void innerUpdateRight(UniTuple<Right_> rightTuple, Consumer<Consumer<LeftTuple_>> leftTupleConsumer) {
        // Prefer an update over retract-insert if possible
        TupleList<OutTuple_> outTupleListRight = rightTuple.getStore(inputStoreIndexRightOutTupleList);
        if (!isFiltering) {
            // Propagate the update for downstream filters, matchWeighers, ...
            for (var outTuple = outTupleListRight.first(); outTuple != null; outTuple = outTupleListRight.next(outTuple)) {
                setOutTupleRightFact(outTuple, rightTuple);
                doUpdateOutTuple(outTuple);
            }
        } else {
            leftTupleConsumer.accept(leftTuple -> processOutTupleUpdateFromLeft(leftTuple, rightTuple,
                    leftTuple.getStore(inputStoreIndexLeftOutTupleList), outTupleListRight, outputStoreIndexLeftOutTupleList));
        }
    }

    private void processOutTupleUpdateFromLeft(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple,
            TupleList<OutTuple_> outList, TupleList<OutTuple_> outTupleList,
            int outputStoreIndexOutEntry) {
        if (!leftTuple.getState().isActive()) {
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

    private void processOutTupleUpdate(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple,
            TupleList<OutTuple_> referenceList, TupleList<OutTuple_> sourceList,
            int outputStoreIndexOutEntry) {
        var outTuple = findOutTuple(sourceList, referenceList, outputStoreIndexOutEntry);
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

    private static <Tuple_ extends Tuple> Tuple_ findOutTuple(TupleList<Tuple_> sourceList,
            TupleList<Tuple_> referenceList, int outputStoreIndexOutEntry) {
        // Hack: the outTuple has no left/right input tuple reference, use the left/right outList reference instead.
        var item = sourceList.first();
        // Safe: findOutTuple returns immediately on a match; the caller retracts after this call returns, not during the walk.
        while (item != null) {
            // Creating list iterators here caused major GC pressure; therefore, we iterate over the entries directly.
            TupleList<Tuple_> outEntryList = item.getStore(outputStoreIndexOutEntry);
            if (referenceList == outEntryList) {
                return item;
            }
            item = sourceList.next(item);
        }
        return null;
    }

    private void retractOutTuple(OutTuple_ outTuple) {
        removeLeftEntry(outTuple);
        removeRightEntry(outTuple);
        propagateRetract(outTuple);
    }

    private void removeLeftEntry(OutTuple_ outTuple) {
        removeEntry(outTuple, outputStoreIndexLeftOutTupleList);
    }

    private void removeRightEntry(OutTuple_ outTuple) {
        removeEntry(outTuple, outputStoreIndexRightOutTupleList);
    }

    private void removeEntry(OutTuple_ outTuple, int outputStoreIndex) {
        TupleList<OutTuple_> list = outTuple.getStore(outputStoreIndex);
        list.remove(outTuple);
        outTuple.setStore(outputStoreIndex, null);
    }

    private void propagateRetract(OutTuple_ outTuple) {
        var state = outTuple.getState();
        if (!state.isActive()) { // Impossible because they shouldn't linger in the indexes.
            throw new IllegalStateException("Impossible state: The tuple (%s) in node (%s) is in an unexpected state (%s)."
                    .formatted(outTuple, this, state));
        }
        propagationQueue.retract(outTuple, state == TupleState.CREATING ? TupleState.ABORTING : TupleState.DYING);
    }

    void retractOutTupleByLeft(OutTuple_ outTuple) {
        outTuple.setStore(outputStoreIndexLeftOutTupleList, null); // The caller will clear the entire list in one go.
        removeRightEntry(outTuple);
        propagateRetract(outTuple);
    }

    void retractOutTupleByRight(OutTuple_ outTuple) {
        removeLeftEntry(outTuple);
        outTuple.setStore(outputStoreIndexRightOutTupleList, null); // The caller will clear the entire list in one go.
        propagateRetract(outTuple);
    }

    @Override
    protected boolean canProduceTuples() {
        return leftCanProduceTuples && rightCanProduceTuples;
    }

    @Override
    public Propagator getPropagator() {
        return propagationQueue;
    }

}
