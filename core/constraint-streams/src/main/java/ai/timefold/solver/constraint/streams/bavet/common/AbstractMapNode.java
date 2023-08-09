package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

public abstract class AbstractMapNode<InTuple_ extends AbstractTuple, OutTuple_ extends AbstractTuple>
        extends AbstractNode<OutTuple_, StaticPropagationQueue<OutTuple_>>
        implements TupleLifecycle<InTuple_> {

    private final int inputStoreIndex;
    protected final int outputStoreSize;
    private final StaticPropagationQueue<OutTuple_> propagationQueue;

    protected AbstractMapNode(int inputStoreIndex, TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, int outputStoreSize) {
        this.inputStoreIndex = inputStoreIndex;
        this.outputStoreSize = outputStoreSize;
        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
    }

    @Override
    public void insert(InTuple_ tuple) {
        if (tuple.getStore(inputStoreIndex) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + tuple
                    + ") was already added in the tupleStore.");
        }
        OutTuple_ outTuple = map(tuple);
        tuple.setStore(inputStoreIndex, outTuple);
        propagationQueue.insert(outTuple);
    }

    protected abstract OutTuple_ map(InTuple_ inTuple);

    @Override
    public void update(InTuple_ tuple) {
        OutTuple_ outTuple = tuple.getStore(inputStoreIndex);
        if (outTuple == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insert(tuple);
            return;
        }
        boolean wasUpdated = remap(tuple, outTuple);
        if (wasUpdated) { // Only propagate if the tuple actually changed.
            TupleState previousState = outTuple.state;
            if (previousState == TupleState.CREATING || previousState == TupleState.UPDATING) {
                // Already in the queue in the correct state.
                return;
            }
            propagationQueue.update(outTuple);
        }
    }

    /**
     * @param inTuple never null; the tuple to apply mappings on
     * @param oldOutTuple never null; the tuple that was previously mapped to the inTuple
     * @return true if oldOutTuple changed during remapping
     */
    protected abstract boolean remap(InTuple_ inTuple, OutTuple_ oldOutTuple);

    @Override
    public void retract(InTuple_ tuple) {
        OutTuple_ outTuple = tuple.removeStore(inputStoreIndex);
        if (outTuple == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        propagationQueue.retract(outTuple, outTuple.state == TupleState.CREATING ? TupleState.ABORTING : TupleState.DYING);
    }

    @Override
    protected final StaticPropagationQueue<OutTuple_> getPropagationQueue() {
        return propagationQueue;
    }
}
