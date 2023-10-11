package ai.timefold.solver.constraint.streams.bavet.common;

import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.ABORTING;
import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.CREATING;
import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.DYING;

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

    protected abstract Tuple_ getOutTuple(Tuple_ inTuple);

    protected abstract void updateOutTuple(Tuple_ inTuple, Tuple_ outTuple);

    @Override
    public final void insertLeft(Tuple_ tuple) {
        Tuple_ outTuple = getOutTuple(tuple);
        tuple.setStore(leftSourceTupleCloneStoreIndex, outTuple);
        propagationQueue.insert(outTuple);
    }

    @Override
    public final void updateLeft(Tuple_ tuple) {
        Tuple_ outTuple = tuple.getStore(leftSourceTupleCloneStoreIndex);
        if (outTuple != null) {
            updateOutTuple(tuple, outTuple);
            propagationQueue.update(outTuple);
        } else {
            // this can happen when left and right have the same TupleSource,
            // and it was inserted on the right (but not the left).
            insertLeft(tuple);
        }
    }

    @Override
    public final void retractLeft(Tuple_ tuple) {
        Tuple_ outTuple = tuple.getStore(leftSourceTupleCloneStoreIndex);
        if (outTuple == null) {
            // this can happen when left and right have the same TupleSource,
            // and it was inserted on the right (but not the left).
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
    public final void insertRight(Tuple_ tuple) {
        Tuple_ outTuple = getOutTuple(tuple);
        tuple.setStore(rightSourceTupleCloneStoreIndex, outTuple);
        propagationQueue.insert(outTuple);
    }

    @Override
    public final void updateRight(Tuple_ tuple) {
        Tuple_ outTuple = tuple.getStore(rightSourceTupleCloneStoreIndex);
        if (outTuple != null) {
            updateOutTuple(tuple, outTuple);
            propagationQueue.update(outTuple);
        } else {
            // this can happen when left and right have the same TupleSource,
            // and it was inserted on the left (but not the right).
            insertRight(tuple);
        }
    }

    @Override
    public final void retractRight(Tuple_ tuple) {
        Tuple_ outTuple = tuple.getStore(rightSourceTupleCloneStoreIndex);
        if (outTuple == null) {
            // this can happen when left and right have the same TupleSource,
            // and it was inserted on the left (but not the right).
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
    public Propagator getPropagator() {
        return propagationQueue;
    }
}
