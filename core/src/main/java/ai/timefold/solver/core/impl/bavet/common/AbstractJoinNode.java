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
        extends AbstractTwoInputNode<LeftTuple_, UniTuple<Right_>>
        implements DeferredSettleAware {

    protected final int inputStoreIndexLeftOutTupleList;
    protected final int inputStoreIndexRightOutTupleList;
    protected final boolean isFiltering;
    private final int outputStoreIndexLeftOutTupleList;
    private final int outputStoreIndexRightOutTupleList;
    protected final OutTupleStorePositionTracker outputStoreSizeTracker;
    private final StaticPropagationQueue<OutTuple_> propagationQueue;
    private long markVersion = 0;

    protected final Supplier<TupleList<OutTuple_>> leftOutTupleListBuilder;
    protected final Supplier<TupleList<OutTuple_>> rightOutTupleListBuilder;

    /**
     * A filtering join whose two inputs are far enough apart (see {@link #canDeferWork()})
     * defers its cross-match computation (the opposite-side walk)
     * from "whenever a parent propagates in" to this node's own layer turn (see {@link #prepareForSettle()}),
     * closing the stale-activity race at its root instead of guarding against it per read.
     * The others read the opposite side on the spot,
     * where the per-read {@code isActive()} guards are enough.
     * Non-filtering joins never dereference a fact through a user predicate,
     * so a stale-but-"active" read can't corrupt anything there (confirmed by dedicated regression tests);
     * these fields stay {@code null}/{@code -1} and cost nothing for them.
     * <p>
     * {@code pendingLeft}/{@code pendingRight} hold tuples whose cross-match is due;
     * the marker slots exist purely to make enqueueing idempotent (a tuple already awaiting its turn isn't re-added).
     * Both lists are drained, left before right, in {@link #prepareForSettle()},
     * calling {@link #reconcilePendingLeft(Tuple)}/{@link #reconcilePendingRight(UniTuple)}.
     * That logic already treats "no existing out-tuple for this pair" as "insert if the predicate passes",
     * so it doubles as the insert path too:
     * whichever side's tuple is reconciled *second* always sees the *first* side's just-created out-tuple
     * (since {@link #insertOutTuple} links it into both sides' out-tuple lists immediately),
     * and correctly treats it as an existing match instead of creating a duplicate.
     * The two sides are therefore never both examined at once;
     * consistently draining left before right merely fixes
     * which side's view is "first" without affecting anything's correctness.
     */
    private final int pendingLeftMarkerIndex;
    private final int pendingRightMarkerIndex;
    protected final @Nullable TupleList<LeftTuple_> pendingLeft;
    protected final @Nullable TupleList<UniTuple<Right_>> pendingRight;

    private long inputLayerDelta = -1;
    private boolean preloadingActive;
    /**
     * Whether {@link #crossMatchLeft}/{@link #crossMatchRight} actually defer,
     * as opposed to reading the opposite side on the spot;
     * precomputed because their callers sit on hot insert and update paths.
     */
    private boolean deferCrossMatch;

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

        if (isFiltering) {
            this.pendingLeftMarkerIndex = tupleStorePositionTracker.reserveNextLeft();
            var pendingLeftPrev = tupleStorePositionTracker.reserveNextLeft();
            var pendingLeftNext = tupleStorePositionTracker.reserveNextLeft();
            this.pendingLeft = new TupleList<>(pendingLeftPrev, pendingLeftNext);
            this.pendingRightMarkerIndex = tupleStorePositionTracker.reserveNextRight();
            var pendingRightPrev = tupleStorePositionTracker.reserveNextRight();
            var pendingRightNext = tupleStorePositionTracker.reserveNextRight();
            this.pendingRight = new TupleList<>(pendingRightPrev, pendingRightNext);
        } else {
            this.pendingLeftMarkerIndex = -1;
            this.pendingLeft = null;
            this.pendingRightMarkerIndex = -1;
            this.pendingRight = null;
        }
    }

    /**
     * Runs {@code leftTuple}'s cross-match (the opposite-side walk).
     * When {@link #deferCrossMatch} says a stale read is possible here,
     * the walk is enqueued for this node's own layer turn instead of running now,
     * unless the tuple is already awaiting one;
     * otherwise it runs on the spot, guarded per read by {@link Tuple#getState()} checks.
     * Only called from filtering code paths.
     */
    protected final void crossMatchLeft(LeftTuple_ leftTuple) {
        if (!deferCrossMatch) {
            reconcilePendingLeft(leftTuple);
        } else if (leftTuple.getStore(pendingLeftMarkerIndex) == null) {
            leftTuple.setStore(pendingLeftMarkerIndex, Boolean.TRUE);
            pendingLeft.add(leftTuple);
        }
    }

    /**
     * The mirror image of {@link #crossMatchLeft}.
     */
    protected final void crossMatchRight(UniTuple<Right_> rightTuple) {
        if (!deferCrossMatch) {
            reconcilePendingRight(rightTuple);
        } else if (rightTuple.getStore(pendingRightMarkerIndex) == null) {
            rightTuple.setStore(pendingRightMarkerIndex, Boolean.TRUE);
            pendingRight.add(rightTuple);
        }
    }

    /**
     * Removes {@code leftTuple} from the pending queue, if it is on it:
     * a tuple can be retracted in the same round it was enqueued, before its turn to reconcile ever comes.
     * Must run before the tuple's own store entries (composite key, out-tuple list, ...) are cleared,
     * since a still-pending entry left dangling would be read by {@link #prepareForSettle()} after those are gone.
     */
    protected final void clearPendingLeft(LeftTuple_ leftTuple) {
        if (pendingLeft != null && leftTuple.getStore(pendingLeftMarkerIndex) != null) {
            leftTuple.setStore(pendingLeftMarkerIndex, null);
            pendingLeft.remove(leftTuple);
        }
    }

    /**
     * The mirror image of {@link #clearPendingLeft}.
     */
    protected final void clearPendingRight(UniTuple<Right_> rightTuple) {
        if (pendingRight != null && rightTuple.getStore(pendingRightMarkerIndex) != null) {
            rightTuple.setStore(pendingRightMarkerIndex, null);
            pendingRight.remove(rightTuple);
        }
    }

    @Override
    public final void setInputLayerDelta(long inputLayerDelta) {
        this.inputLayerDelta = inputLayerDelta;
        recomputeDeferCrossMatch();
    }

    @Override
    public void preloadStarted() {
        if (preloadingActive) {
            throw new IllegalStateException("Impossible state: node (%s) is already preloading.".formatted(this));
        } else {
            this.preloadingActive = true;
            recomputeDeferCrossMatch();
        }
    }

    @Override
    public void preloadEnded() {
        if (!preloadingActive) {
            throw new IllegalStateException("Impossible state: node (%s) is not preloading.".formatted(this));
        } else {
            this.preloadingActive = false;
            recomputeDeferCrossMatch();
        }
    }

    private void recomputeDeferCrossMatch() {
        this.deferCrossMatch = canDeferWork() && !preloadingActive;
    }

    @Override
    public final boolean canDeferWork() {
        if (isFiltering && inputLayerDelta < 0) {
            throw new IllegalStateException("Impossible state: the input layer delta of node (%s) was never set."
                    .formatted(this));
        }
        // A filtering node whose inputs are at most one layer apart can only ever read a tuple
        // that has already been told it is doomed, which the per-read isActive() guards catch.
        // Two or more layers apart, the doom may not have propagated that far yet; only then is deferral needed.
        return isFiltering && inputLayerDelta >= 2;
    }

    @Override
    public final void prepareForSettle() {
        if (pendingLeft == null) { // Non-filtering: nothing was ever enqueued.
            return;
        }
        pendingLeft.clear(leftTuple -> {
            leftTuple.setStore(pendingLeftMarkerIndex, null);
            reconcilePendingLeft(leftTuple);
        });
        pendingRight.clear(rightTuple -> {
            rightTuple.setStore(pendingRightMarkerIndex, null);
            reconcilePendingRight(rightTuple);
        });
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
    protected abstract void reconcilePendingLeft(LeftTuple_ leftTuple);

    /**
     * The mirror image of {@link #reconcilePendingLeft}.
     */
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
