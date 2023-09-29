package ai.timefold.solver.constraint.streams.bavet.common;

import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.ABORTING;
import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.CREATING;
import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.DYING;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

public abstract class AbstractConcatNode<Tuple_ extends AbstractTuple>
        extends AbstractNode
        implements LeftTupleLifecycle<Tuple_>, RightTupleLifecycle<Tuple_> {
    private final int inputStoreIndexLeftOutTupleList;
    private final int inputStoreIndexRightOutTupleList;
    protected final int outputStoreSize;
    private final StaticPropagationQueue<Tuple_> propagationQueue;

    protected AbstractConcatNode(TupleLifecycle<Tuple_> nextNodesTupleLifecycle,
            int inputStoreIndexLeftOutTupleList,
            int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
        this.inputStoreIndexLeftOutTupleList = inputStoreIndexLeftOutTupleList;
        this.inputStoreIndexRightOutTupleList = inputStoreIndexRightOutTupleList;
        this.outputStoreSize = outputStoreSize;
    }

    protected abstract Tuple_ getOutTuple(Tuple_ inTuple);

    @Override
    public final void insertLeft(Tuple_ tuple) {
        Tuple_ outTuple = getOutTuple(tuple);
        tuple.setStore(inputStoreIndexLeftOutTupleList, outTuple);
        propagationQueue.insert(outTuple);
    }

    @Override
    public final void updateLeft(Tuple_ tuple) {
        Tuple_ outTuple = tuple.getStore(inputStoreIndexLeftOutTupleList);
        if (outTuple != null) {
            propagationQueue.update(outTuple);
        } else {
            insertLeft(tuple);
        }
    }

    @Override
    public final void retractLeft(Tuple_ tuple) {
        Tuple_ outTuple = tuple.getStore(inputStoreIndexLeftOutTupleList);
        if (outTuple == null) {
            return;
        }

        TupleState state = outTuple.state;
        if (!state.isActive()) {
            // Impossible because they shouldn't linger in the indexes.
            throw new IllegalStateException("Impossible state: The tuple (" + outTuple.state + ") in node (" + this
                    + ") is in an unexpected state (" + outTuple.state + ").");
        }
        propagationQueue.retract(outTuple, state == CREATING ? ABORTING : DYING);
    }

    @Override
    public final void insertRight(Tuple_ tuple) {
        Tuple_ outTuple = getOutTuple(tuple);
        tuple.setStore(inputStoreIndexRightOutTupleList, outTuple);
        propagationQueue.insert(outTuple);
    }

    @Override
    public final void updateRight(Tuple_ tuple) {
        Tuple_ outTuple = tuple.getStore(inputStoreIndexRightOutTupleList);
        if (outTuple != null) {
            propagationQueue.update(outTuple);
        } else {
            insertRight(tuple);
        }
    }

    @Override
    public final void retractRight(Tuple_ tuple) {
        Tuple_ outTuple = tuple.getStore(inputStoreIndexRightOutTupleList);
        if (outTuple == null) {
            return;
        }

        TupleState state = outTuple.state;
        if (!state.isActive()) {
            // Impossible because they shouldn't linger in the indexes.
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
