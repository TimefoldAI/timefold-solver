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
        extends AbstractCrossMatchNode<LeftTuple_, Right_> {

    protected final int inputStoreIndexLeftOutTupleList;
    protected final int inputStoreIndexRightOutTupleList;
    private final int outputStoreIndexLeftOutTupleList;
    private final int outputStoreIndexRightOutTupleList;
    protected final OutTupleStorePositionTracker outputStoreSizeTracker;
    private final StaticPropagationQueue<OutTuple_> propagationQueue;
    private long markVersion = 0;

    protected final Supplier<TupleList<OutTuple_>> leftOutTupleListBuilder;
    protected final Supplier<TupleList<OutTuple_>> rightOutTupleListBuilder;

    protected AbstractJoinNode(TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, boolean isFiltering,
            InOutTupleStorePositionTracker tupleStorePositionTracker) {
        super(nextNodesTupleLifecycle, isFiltering, tupleStorePositionTracker, false);
        this.inputStoreIndexLeftOutTupleList = tupleStorePositionTracker.reserveNextLeft();
        this.inputStoreIndexRightOutTupleList = tupleStorePositionTracker.reserveNextRight();
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

    /**
     * Re-runs this left tuple's cross-match against the current
     * (now fully settled, for this round)
     * opposite side, exactly as an eager filtering update already would have ({@link #innerUpdateLeft});
     * reusing that method is what makes this correct for a tuple that was actually a fresh insert too:
     * with no pre-existing out-tuples to mark, every match it finds is necessarily new,
     * so {@code innerUpdateLeft}'s own "no existing out-tuple ⇒
     * insert" branch handles it.
     * Implemented by each subclass because only it knows how to walk the opposite side
     * (indexed: the shared index/bucket; unindexed: the plain tuple list).
     */
    @Override
    protected abstract void reconcilePendingLeft(LeftTuple_ leftTuple);

    /**
     * The mirror image of {@link #reconcilePendingLeft}.
     */
    @Override
    protected abstract void reconcilePendingRight(UniTuple<Right_> rightTuple);

    @Override
    public StreamKind getStreamKind() {
        return StreamKind.JOIN;
    }

    protected abstract OutTuple_ createOutTuple(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple);

    protected abstract void setOutTupleLeftFacts(OutTuple_ outTuple, LeftTuple_ leftTuple);

    protected abstract void setOutTupleRightFact(OutTuple_ outTuple, UniTuple<Right_> rightTuple);

    protected abstract boolean testFiltering(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple);

    /**
     * Only ever called from the non-filtering path
     * (filtering joins route through {@link #crossMatchLeft}/{@link #crossMatchRight} instead),
     * where {@code testFiltering(...)} is never even consulted;
     * see {@link #testFiltering}'s callers.
     */
    protected final void insertOutTupleIfActiveFiltered(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        if (!isFiltering || testFiltering(leftTuple, rightTuple)) {
            insertOutTuple(leftTuple, rightTuple);
        }
    }

    private void insertOutTuple(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple) {
        var outTuple = createOutTuple(leftTuple, rightTuple);
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
            if (!leftTuple.getState().isActive()) {
                // See insertOutTupleIfActiveFiltered(...): the out-tuple must be retracted anyway,
                // so any further update to it is pointless.
                // Unreachable when this runs from reconcilePendingLeft, at this node's own layer turn.
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
        if (!rightTuple.getState().isActive()) {
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
            if (!rightTuple.getState().isActive()) {
                // The mirror image of innerUpdateLeft(...): here the right tuple is the retracting one,
                // and its out-tuples are about to be retracted regardless of what the predicate now says.
                return;
            }
            var version = ++markVersion;
            for (var outTuple = outTupleListRight.first(); outTuple != null; outTuple = outTupleListRight.next(outTuple)) {
                TupleList<OutTuple_> outTupleListLeft = outTuple.getStore(outputStoreIndexLeftOutTupleList);
                outTupleListLeft.mark(outTuple, version);
            }
            leftTupleConsumer.accept(leftTuple -> processOutTupleUpdateLeft(leftTuple, rightTuple, version));
        }
    }

    private void processOutTupleUpdateLeft(LeftTuple_ leftTuple, UniTuple<Right_> rightTuple, long version) {
        if (!leftTuple.getState().isActive()) {
            // See insertOutTupleIfActiveFiltered(...): the out-tuple must be retracted anyway,
            // so any further update to it is pointless.
            // Unreachable when this runs from reconcilePendingRight, at this node's own layer turn.
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
