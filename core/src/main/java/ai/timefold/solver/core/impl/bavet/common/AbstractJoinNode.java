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
public abstract class AbstractJoinNode<LeftTuple_ extends Tuple, Right_, OutTuple_ extends Tuple>
        extends AbstractTwoInputNode<LeftTuple_, UniTuple<Right_>> {

    protected final int inputStoreIndexLeftOutTupleList;
    protected final int inputStoreIndexRightOutTupleList;
    private final boolean isFiltering;
    private final int outputStoreIndexLeftOutTupleList;
    private final int outputStoreIndexRightOutTupleList;
    protected final OutTupleStorePositionTracker outputStoreSizeTracker;
    private final StaticPropagationQueue<OutTuple_> propagationQueue;
    private long markVersion = 0;

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

    protected final void insertOutTupleFilteredLeft(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        if (isFiltering && !leftTuple.isActiveTransitively()) {
            // See Tuple#isActiveTransitively for why the immediate state alone isn't enough here.
            // Non-filtering joins never dereference a fact through a user predicate, so a stale-but-
            // "active" read here cannot corrupt anything: it merely produces an out-tuple that the true
            // retraction (arriving later, deeper in the same layer chain) cleans up via the retracted
            // tuple's own out-tuple list. The guard is therefore scoped to filtering joins only, where
            // testFiltering(...) below would otherwise run against stale data.
            return;
        }
        insertOutTupleIfActiveFiltered(leftTuple, rightTuple);
    }

    /**
     * The mirror image of {@link #insertOutTupleFilteredLeft}:
     * the right tuple is the one read out of storage, and can therefore be the retracting one.
     * See {@link Tuple#isActiveTransitively} for why the immediate state alone isn't enough here.
     */
    protected final void insertOutTupleFilteredRight(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        if (isFiltering && !rightTuple.isActiveTransitively()) {
            return;
        }
        insertOutTupleIfActiveFiltered(leftTuple, rightTuple);
    }

    private void insertOutTupleIfActiveFiltered(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        if (!isFiltering || testFiltering(leftTuple, rightTuple)) {
            insertOutTuple(leftTuple, rightTuple);
        }
    }

    private void insertOutTuple(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        var outTuple = createOutTuple(leftTuple, rightTuple);
        outTuple.setActivityParents(leftTuple, rightTuple);
        TupleList<OutTuple_> outTupleListLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
        outTupleListLeft.add(outTuple);
        outTuple.setStore(outputStoreIndexLeftOutTupleList, outTupleListLeft);
        TupleList<OutTuple_> outTupleListRight = rightTuple.getStore(inputStoreIndexRightOutTupleList);
        outTupleListRight.add(outTuple);
        outTuple.setStore(outputStoreIndexRightOutTupleList, outTupleListRight);
        propagationQueue.insert(outTuple);
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
            if (!leftTuple.isActiveTransitively()) {
                // See Tuple#isActiveTransitively for why the immediate state alone isn't enough here.
                return;
            }
            // Every out-tuple's partner is guaranteed to be swept below,
            // because retracts and key-moves unlink out-tuples synchronously;
            // a stale mark can therefore only ever be version-mismatched.
            var version = ++markVersion;
            for (var outTuple = outTupleListLeft.first(); outTuple != null; outTuple = outTupleListLeft.next(outTuple)) {
                TupleList<OutTuple_> outTupleListRight = outTuple.getStore(outputStoreIndexRightOutTupleList);
                outTupleListRight.mark(outTuple, version);
            }
            rightTupleConsumer.accept(rightTuple -> processOutTupleUpdateRight(leftTuple, rightTuple, version));
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

    private void processOutTupleUpdateRight(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple, long version) {
        if (!rightTuple.isActiveTransitively()) {
            // The mirror image of processOutTupleUpdateLeft(...): here the right tuple is the retracting one.
            // Leaving its mark set is harmless, as getMark() only ever returns a mark of the matching version.
            return;
        }
        TupleList<OutTuple_> outTupleListRight = rightTuple.getStore(inputStoreIndexRightOutTupleList);
        processOutTupleUpdate(leftTuple, rightTuple, outTupleListRight.getMark(version));
    }

    private void processOutTupleUpdate(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple, @Nullable OutTuple_ outTuple) {
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

    private void retractOutTuple(OutTuple_ outTuple) {
        removeLeftEntry(outTuple);
        removeRightEntry(outTuple);
        propagateRetract(outTuple);
    }

    private void removeLeftEntry(OutTuple_ outTuple) {
        removeEntry(outTuple, outputStoreIndexLeftOutTupleList);
    }

    private void removeEntry(OutTuple_ outTuple, int outputStoreIndex) {
        TupleList<OutTuple_> list = outTuple.getStore(outputStoreIndex);
        list.remove(outTuple);
        outTuple.setStore(outputStoreIndex, null);
    }

    private void removeRightEntry(OutTuple_ outTuple) {
        removeEntry(outTuple, outputStoreIndexRightOutTupleList);
    }

    private void propagateRetract(OutTuple_ outTuple) {
        var state = outTuple.getState();
        if (!state.isActive()) { // Impossible because they shouldn't linger in the indexes.
            throw new IllegalStateException("Impossible state: The tuple (%s) in node (%s) is in an unexpected state (%s)."
                    .formatted(outTuple, this, state));
        }
        propagationQueue.retract(outTuple, state == TupleState.CREATING ? TupleState.ABORTING : TupleState.DYING);
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
            var version = ++markVersion;
            for (var outTuple = outTupleListRight.first(); outTuple != null; outTuple = outTupleListRight.next(outTuple)) {
                TupleList<OutTuple_> outTupleListLeft = outTuple.getStore(outputStoreIndexLeftOutTupleList);
                outTupleListLeft.mark(outTuple, version);
            }
            leftTupleConsumer.accept(leftTuple -> processOutTupleUpdateLeft(leftTuple, rightTuple, version));
        }
    }

    private void processOutTupleUpdateLeft(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple, long version) {
        if (!leftTuple.isActiveTransitively()) {
            // See Tuple#isActiveTransitively for why the immediate state alone isn't enough here.
            return;
        }
        TupleList<OutTuple_> outTupleListLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
        processOutTupleUpdateRight(leftTuple, rightTuple, outTupleListLeft.getMark(version));
    }

    private void processOutTupleUpdateRight(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple, @Nullable OutTuple_ outTuple) {
        if (testFiltering(leftTuple, rightTuple)) {
            if (outTuple == null) {
                insertOutTuple(leftTuple, rightTuple);
            } else {
                updateOutTupleRight(outTuple, rightTuple);
            }
        } else {
            if (outTuple != null) {
                retractOutTuple(outTuple);
            }
        }
    }

    private void updateOutTupleRight(OutTuple_ outTuple, UniTuple<Right_> rightTuple) {
        setOutTupleRightFact(outTuple, rightTuple);
        doUpdateOutTuple(outTuple);
    }

    protected void retractOutTupleLeft(OutTuple_ outTuple) {
        outTuple.setStore(outputStoreIndexLeftOutTupleList, null); // The caller will clear the entire list in one go.
        removeRightEntry(outTuple);
        propagateRetract(outTuple);
    }

    protected void retractOutTupleRight(OutTuple_ outTuple) {
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
