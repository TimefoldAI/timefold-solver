package ai.timefold.solver.core.impl.bavet.common;

import java.util.function.Consumer;

import ai.timefold.solver.core.impl.bavet.common.index.IndexedSet;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.OutputStoreSizeTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.jspecify.annotations.Nullable;

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

    protected final int inputStoreIndexLeftOutTupleSet;
    protected final int inputStoreIndexRightOutTupleSet;
    private final boolean isFiltering;
    private final int outputStoreIndexLeftOutSet;
    private final int outputStoreIndexRightOutSet;
    protected final OutputStoreSizeTracker outputStoreSizeTracker;
    private final StaticPropagationQueue<OutTuple_> propagationQueue;

    protected AbstractJoinNode(TupleStorePositionTracker leftTupleStorePositionTracker,
            TupleStorePositionTracker rightTupleStorePositionTracker, OutputStoreSizeTracker outputStoreSizeTracker,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, boolean isFiltering) {
        this.inputStoreIndexLeftOutTupleSet = leftTupleStorePositionTracker.reserveNextAvailablePosition();
        this.inputStoreIndexRightOutTupleSet = rightTupleStorePositionTracker.reserveNextAvailablePosition();
        this.isFiltering = isFiltering;
        this.outputStoreIndexLeftOutSet = outputStoreSizeTracker.reserveNextAvailablePosition();
        this.outputStoreIndexRightOutSet = outputStoreSizeTracker.reserveNextAvailablePosition();
        this.outputStoreSizeTracker = outputStoreSizeTracker;
        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
    }

    protected abstract OutTuple_ createOutTuple(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple);

    protected abstract void setOutTupleLeftFacts(OutTuple_ outTuple, LeftTuple_ leftTuple);

    protected abstract void setOutTupleRightFact(OutTuple_ outTuple, UniTuple<Right_> rightTuple);

    protected abstract boolean testFiltering(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple);

    protected final void insertOutTuple(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        var outTuple = createOutTuple(leftTuple, rightTuple);
        IndexedSet<OutTuple_> outTupleSetLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleSet);
        outTupleSetLeft.add(outTuple);
        outTuple.setStore(outputStoreIndexLeftOutSet, outTupleSetLeft);
        IndexedSet<OutTuple_> outTupleSetRight = rightTuple.getStore(inputStoreIndexRightOutTupleSet);
        outTupleSetRight.add(outTuple);
        outTuple.setStore(outputStoreIndexRightOutSet, outTupleSetRight);
        propagationQueue.insert(outTuple);
    }

    protected final void insertOutTupleFiltered(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
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
        if (!isFiltering || testFiltering(leftTuple, rightTuple)) {
            insertOutTuple(leftTuple, rightTuple);
        }
    }

    protected final void innerUpdateLeft(LeftTuple_ leftTuple, Consumer<Consumer<UniTuple<Right_>>> rightTupleConsumer) {
        // Prefer an update over retract-insert if possible
        IndexedSet<OutTuple_> outTupleSetLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleSet);
        // Propagate the update for downstream filters, matchWeighers, ...
        if (!isFiltering) {
            outTupleSetLeft.forEach(outTuple -> updateOutTupleLeft(outTuple, leftTuple));
        } else {
            rightTupleConsumer.accept(rightTuple -> {
                IndexedSet<OutTuple_> outTupleSetRight = rightTuple.getStore(inputStoreIndexRightOutTupleSet);
                processOutTupleUpdate(leftTuple, rightTuple, outTupleSetRight, outTupleSetLeft, outputStoreIndexRightOutSet);
            });
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
        IndexedSet<OutTuple_> outTupleSetRight = rightTuple.getStore(inputStoreIndexRightOutTupleSet);
        if (!isFiltering) {
            // Propagate the update for downstream filters, matchWeighers, ...
            outTupleSetRight.forEach(outTuple -> {
                setOutTupleRightFact(outTuple, rightTuple);
                doUpdateOutTuple(outTuple);
            });
        } else {
            leftTupleConsumer.accept(leftTuple -> {
                IndexedSet<OutTuple_> outTupleSetLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleSet);
                processOutTupleUpdate(leftTuple, rightTuple, outTupleSetLeft, outTupleSetRight, outputStoreIndexLeftOutSet);
            });
        }
    }

    private void processOutTupleUpdate(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple,
            IndexedSet<OutTuple_> referenceOutTupleSet, IndexedSet<OutTuple_> outTupleSet,
            int outputStoreIndexOutSet) {
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
        var outTuple = findOutTuple(outTupleSet, referenceOutTupleSet, outputStoreIndexOutSet);
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

    private static <Tuple_ extends AbstractTuple> @Nullable Tuple_ findOutTuple(IndexedSet<Tuple_> outTupleSet,
            IndexedSet<Tuple_> referenceOutTupleSet, int outputStoreIndexOutSet) {
        // Hack: the outTuple has no left/right input tuple reference, use the left/right outSet reference instead.
        var list = outTupleSet.asList();
        for (var i = 0; i < list.size(); i++) { // Avoid allocating iterators.
            var outTuple = list.get(i);
            if (referenceOutTupleSet == outTuple.getStore(outputStoreIndexOutSet)) {
                return outTuple;
            }
        }
        return null;
    }

    protected final void retractOutTuple(OutTuple_ outTuple) {
        IndexedSet<OutTuple_> outSetLeft = outTuple.removeStore(outputStoreIndexLeftOutSet);
        outSetLeft.remove(outTuple);
        IndexedSet<OutTuple_> outSetRight = outTuple.removeStore(outputStoreIndexRightOutSet);
        outSetRight.remove(outTuple);
        var state = outTuple.state;
        if (!state.isActive()) { // Impossible because they shouldn't linger in the indexes.
            throw new IllegalStateException("Impossible state: The tuple (%s) in node (%s) is in an unexpected state (%s)."
                    .formatted(outTuple, this, outTuple.state));
        }
        propagationQueue.retract(outTuple, state == TupleState.CREATING ? TupleState.ABORTING : TupleState.DYING);
    }

    @Override
    public Propagator getPropagator() {
        return propagationQueue;
    }

}
