package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.ArrayDeque;
import java.util.Queue;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

public abstract class AbstractMapNode<InTuple_ extends AbstractTuple, OutTuple_ extends AbstractTuple>
        extends AbstractNode
        implements TupleLifecycle<InTuple_> {

    private final int inputStoreIndex;
    /**
     * Calls for example {@link AbstractScorer#insert(AbstractTuple)} and/or ...
     */
    private final TupleLifecycle<OutTuple_> nextNodesTupleLifecycle;
    protected final int outputStoreSize;
    private final Queue<OutTuple_> dirtyTupleQueue;

    protected AbstractMapNode(int inputStoreIndex, TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, int outputStoreSize) {
        this.inputStoreIndex = inputStoreIndex;
        this.nextNodesTupleLifecycle = nextNodesTupleLifecycle;
        this.outputStoreSize = outputStoreSize;
        dirtyTupleQueue = new ArrayDeque<>(1000);
    }

    @Override
    public void insert(InTuple_ tuple) {
        if (tuple.getStore(inputStoreIndex) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + tuple
                    + ") was already added in the tupleStore.");
        }
        OutTuple_ outTuple = map(tuple);
        tuple.setStore(inputStoreIndex, outTuple);
        dirtyTupleQueue.add(outTuple);
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
        if (remap(tuple, outTuple)) {
            outTuple.setState(TupleState.UPDATING);
            dirtyTupleQueue.add(outTuple);
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
        outTuple.setState(TupleState.DYING);
        dirtyTupleQueue.add(outTuple);
    }

    @Override
    public void calculateScore() {
        for (OutTuple_ tuple : dirtyTupleQueue) {
            switch (tuple.getState()) {
                case CREATING:
                    nextNodesTupleLifecycle.insert(tuple);
                    tuple.setState(TupleState.OK);
                    break;
                case UPDATING:
                    nextNodesTupleLifecycle.update(tuple);
                    tuple.setState(TupleState.OK);
                    break;
                case DYING:
                    nextNodesTupleLifecycle.retract(tuple);
                    tuple.setState(TupleState.DEAD);
                    break;
                case ABORTING:
                    tuple.setState(TupleState.DEAD);
                    break;
                case OK:
                case DEAD:
                default:
                    throw new IllegalStateException("Impossible state: The tuple (" + tuple + ") in node (" +
                            this + ") is in an unexpected state (" + tuple.getState() + ").");
            }
        }
        dirtyTupleQueue.clear();
    }

}
