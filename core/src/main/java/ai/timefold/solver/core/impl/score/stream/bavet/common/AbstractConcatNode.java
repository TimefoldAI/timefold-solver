package ai.timefold.solver.core.impl.score.stream.bavet.common;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleState;

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
public abstract class AbstractConcatNode<LeftTuple_ extends AbstractTuple, RightTuple_ extends AbstractTuple, OutTuple_ extends AbstractTuple>
        extends AbstractNode
        implements LeftTupleLifecycle<LeftTuple_>, RightTupleLifecycle<RightTuple_> {
    private final int leftSourceTupleCloneStoreIndex;
    private final int rightSourceTupleCloneStoreIndex;
    protected final int outputStoreSize;
    private final StaticPropagationQueue<OutTuple_> propagationQueue;

    protected AbstractConcatNode(TupleLifecycle<OutTuple_> nextNodesTupleLifecycle,
            int leftSourceTupleCloneStoreIndex,
            int rightSourceTupleCloneStoreIndex,
            int outputStoreSize) {
        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
        this.leftSourceTupleCloneStoreIndex = leftSourceTupleCloneStoreIndex;
        this.rightSourceTupleCloneStoreIndex = rightSourceTupleCloneStoreIndex;
        this.outputStoreSize = outputStoreSize;
    }

    protected abstract OutTuple_ getOutTupleFromLeft(LeftTuple_ leftTuple);

    protected abstract OutTuple_ getOutTupleFromRight(RightTuple_ rightTuple);

    protected abstract void updateOutTupleFromLeft(LeftTuple_ leftTuple, OutTuple_ outTuple);

    protected abstract void updateOutTupleFromRight(RightTuple_ rightTuple, OutTuple_ outTuple);

    @Override
    public final void insertLeft(LeftTuple_ tuple) {
        OutTuple_ outTuple = getOutTupleFromLeft(tuple);
        tuple.setStore(leftSourceTupleCloneStoreIndex, outTuple);
        propagationQueue.insert(outTuple);
    }

    @Override
    public final void updateLeft(LeftTuple_ tuple) {
        OutTuple_ outTuple = tuple.getStore(leftSourceTupleCloneStoreIndex);
        if (outTuple == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertLeft(tuple);
            return;
        }

        updateOutTupleFromLeft(tuple, outTuple);
        // Even if the facts of tuple do not change, an update MUST be done so
        // downstream nodes get notified of updates in planning variables.
        TupleState previousState = outTuple.state;
        if (previousState == TupleState.CREATING || previousState == TupleState.UPDATING) {
            return;
        }
        propagationQueue.update(outTuple);
    }

    @Override
    public final void retractLeft(LeftTuple_ tuple) {
        OutTuple_ outTuple = tuple.getStore(leftSourceTupleCloneStoreIndex);
        if (outTuple == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        TupleState state = outTuple.state;
        if (!state.isActive()) {
            // No fail fast for inactive tuples, since the same tuple can be
            // passed twice if they are from the same source;
            // @see BavetRegressionTest#concatSameTupleDeadAndAlive for an example.
            return;
        }
        propagationQueue.retract(outTuple, state == TupleState.CREATING ? TupleState.ABORTING : TupleState.DYING);
    }

    @Override
    public final void insertRight(RightTuple_ tuple) {
        OutTuple_ outTuple = getOutTupleFromRight(tuple);
        tuple.setStore(rightSourceTupleCloneStoreIndex, outTuple);
        propagationQueue.insert(outTuple);
    }

    @Override
    public final void updateRight(RightTuple_ tuple) {
        OutTuple_ outTuple = tuple.getStore(rightSourceTupleCloneStoreIndex);
        if (outTuple == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertRight(tuple);
            return;
        }

        updateOutTupleFromRight(tuple, outTuple);
        // Even if the facts of tuple do not change, an update MUST be done so
        // downstream nodes get notified of updates in planning variables.
        TupleState previousState = outTuple.state;
        if (previousState == TupleState.CREATING || previousState == TupleState.UPDATING) {
            return;
        }
        propagationQueue.update(outTuple);
    }

    @Override
    public final void retractRight(RightTuple_ tuple) {
        OutTuple_ outTuple = tuple.getStore(rightSourceTupleCloneStoreIndex);
        if (outTuple == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        TupleState state = outTuple.state;
        if (!state.isActive()) {
            // No fail fast for inactive tuples, since the same tuple can be
            // passed twice if they are from the same source;
            // @see BavetRegressionTest#concatSameTupleDeadAndAlive for an example.
            return;
        }
        propagationQueue.retract(outTuple, state == TupleState.CREATING ? TupleState.ABORTING : TupleState.DYING);
    }

    @Override
    public Propagator getPropagator() {
        return propagationQueue;
    }
}
