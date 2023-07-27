package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

public abstract class AbstractFilterNode<Tuple_ extends AbstractTuple>
        extends AbstractNode
        implements TupleLifecycle<Tuple_> {

    private final int inputStoreIndex;
    /**
     * Calls for example {@link AbstractScorer#insert(AbstractTuple)} and/or ...
     */
    private final TupleLifecycle<Tuple_> nextNodesTupleLifecycle;
    private final DirtyQueue<Tuple_, Tuple_> dirtyTupleQueue;

    protected AbstractFilterNode(int inputStoreIndex, TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        this.inputStoreIndex = inputStoreIndex;
        this.nextNodesTupleLifecycle = nextNodesTupleLifecycle;
        dirtyTupleQueue = DirtyQueue.ofTuples();
    }

    @Override
    public void insert(Tuple_ tuple) {
        if (tuple.getStore(inputStoreIndex) != null) {
            throw new IllegalStateException("Impossible state: the output for the tuple (" + tuple
                    + ") was already added in the tupleStore.");
        }
        if (testFiltering(tuple)) {
            Tuple_ outTuple = clone(tuple);
            tuple.setStore(inputStoreIndex, outTuple);
            dirtyTupleQueue.insertWithState(outTuple, TupleState.CREATING);
        }
    }

    protected abstract Tuple_ clone(Tuple_ inTuple);

    protected abstract void remap(Tuple_ inTuple, Tuple_ outTuple);

    protected abstract boolean testFiltering(Tuple_ tuple);

    @Override
    public void update(Tuple_ tuple) {
        Tuple_ outTuple = tuple.getStore(inputStoreIndex);
        if (outTuple == null) { // The tuple was never inserted because it did not pass the filter.
            insert(tuple);
        } else if (testFiltering(tuple)) {
            remap(tuple, outTuple);
            dirtyTupleQueue.insertWithState(outTuple, TupleState.UPDATING);
        } else {
            retract(tuple);
        }
    }

    @Override
    public void retract(Tuple_ tuple) {
        Tuple_ outTuple = tuple.removeStore(inputStoreIndex);
        if (outTuple == null) { // The tuple was never inserted because it did not pass the filter.
            return;
        }
        dirtyTupleQueue.insertWithState(outTuple,
                outTuple.state == TupleState.CREATING ? TupleState.ABORTING : TupleState.DYING);
    }

    @Override
    public void calculateScore() {
        dirtyTupleQueue.clear(this, nextNodesTupleLifecycle);
    }

}
