package ai.timefold.solver.constraint.streams.bavet.common;

import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.ABORTING;
import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.CREATING;
import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.DYING;
import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.UPDATING;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

/**
 * Implements the concat operation. Concat cannot be implemented as a pass-through operation because of two caveats:
 *
 * <ul>
 * <li>It is possible to have the same {@link TupleSource} for both parent streams,
 * in which case the exact same tuple can be inserted twice. Such a tuple
 * should be counted twice downstream, and thus need to be cloned.
 * </li>
 *
 * <li>Because concat has two parent nodes, it must be a {@link TupleSource} (since
 * all nodes have exactly one {@link TupleSource}, and the source tuple can come from
 * either parent). {@link TupleSource} must produce new tuples and not reuse them, since
 * if tuples are reused, the stores inside them get corrupted.
 * </li>
 * </ul>
 *
 * The {@link AbstractConcatNode} works by creating a copy of the source tuple and putting it into
 * the tuple's store. If the same tuple is inserted twice (i.e. when the left and right parent
 * have the same {@link TupleSource}), it creates another clone.
 */
public abstract class AbstractConcatNode<Tuple_ extends AbstractTuple>
        extends AbstractNode
        implements LeftTupleLifecycle<Tuple_>, RightTupleLifecycle<Tuple_> {
    private final int leftSourceTupleCloneStoreIndex;
    private final int rightSourceTupleCloneStoreIndex;
    protected final int outputStoreSize;
    private final StaticPropagationQueue<Tuple_> propagationQueue;

    protected AbstractConcatNode(TupleLifecycle<Tuple_> nextNodesTupleLifecycle,
            int leftSourceTupleCloneStoreIndex,
            int rightSourceTupleCloneStoreIndex,
            int outputStoreSize) {
        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
        this.leftSourceTupleCloneStoreIndex = leftSourceTupleCloneStoreIndex;
        this.rightSourceTupleCloneStoreIndex = rightSourceTupleCloneStoreIndex;
        this.outputStoreSize = outputStoreSize;
    }

    /**
     * Creates a copy of the inTuple with the same fact (and new store/state).
     */
    protected abstract Tuple_ getOutTuple(Tuple_ inTuple);

    /**
     * Updates outTuple to contain the same facts as inTuple.
     */
    protected abstract void updateOutTuple(Tuple_ inTuple, Tuple_ outTuple);

    private void insert(Tuple_ tuple, int storeIndex) {
        Tuple_ outTuple = getOutTuple(tuple);
        tuple.setStore(storeIndex, outTuple);
        propagationQueue.insert(outTuple);
    }

    private void update(Tuple_ tuple, int storeIndex) {
        Tuple_ outTuple = tuple.getStore(storeIndex);
        if (outTuple == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insert(tuple, storeIndex);
            return;
        }

        updateOutTuple(tuple, outTuple);
        // Even if the facts of tuple do not change, an update MUST be done so
        // downstream nodes get notified of updates in planning variables.
        TupleState previousState = outTuple.state;
        if (previousState == CREATING || previousState == UPDATING) {
            return;
        }
        propagationQueue.update(outTuple);
    }

    private void retract(Tuple_ tuple, int storeIndex) {
        Tuple_ outTuple = tuple.getStore(storeIndex);
        if (outTuple == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        TupleState state = outTuple.state;
        if (!state.isActive()) {
            throw new IllegalStateException("Impossible state: The tuple (" + outTuple.state + ") in node (" + this
                    + ") is in an unexpected state (" + outTuple.state + ").");
        }
        propagationQueue.retract(outTuple, state == CREATING ? ABORTING : DYING);
    }

    @Override
    public final void insertLeft(Tuple_ tuple) {
        insert(tuple, leftSourceTupleCloneStoreIndex);
    }

    @Override
    public final void updateLeft(Tuple_ tuple) {
        update(tuple, leftSourceTupleCloneStoreIndex);
    }

    @Override
    public final void retractLeft(Tuple_ tuple) {
        retract(tuple, leftSourceTupleCloneStoreIndex);
    }

    @Override
    public final void insertRight(Tuple_ tuple) {
        insert(tuple, rightSourceTupleCloneStoreIndex);
    }

    @Override
    public final void updateRight(Tuple_ tuple) {
        update(tuple, rightSourceTupleCloneStoreIndex);
    }

    @Override
    public final void retractRight(Tuple_ tuple) {
        retract(tuple, rightSourceTupleCloneStoreIndex);
    }

    @Override
    public Propagator getPropagator() {
        return propagationQueue;
    }
}
