package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

/**
 * Filter node is a pass-through node which does not propagate tuples that do not pass a predicate.
 * It is designed not to create tuples, as that would become a very expensive operation.
 * This has a consequence on how tuple state is stored.
 * By the time a tuple makes it to {@link #insert(AbstractTuple)}, {@link #update(AbstractTuple)} or
 * {@link #retract(AbstractTuple)},
 * its state is already {@link TupleState#OK} from the input node.
 * Therefore, in order not to have to create a clone of the tuple,
 * this node's tuple state is stored in {@link AbstractTuple}'s state store as opposed to {@link AbstractTuple#state}.
 *
 * @param <Tuple_>
 */
public abstract class AbstractFilterNode<Tuple_ extends AbstractTuple>
        extends AbstractNode
        implements TupleLifecycle<Tuple_> {

    private final int tupleStateStoreIndex;
    private final FilterDirtyQueue<Tuple_> dirtyTupleQueue;

    protected AbstractFilterNode(int tupleStateStoreIndex, TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        this.tupleStateStoreIndex = tupleStateStoreIndex;
        /*
         * The tuple state is stored through the queue, which needs to know it for score calculation.
         * Read operations happen through the tuple directly.
         */
        this.dirtyTupleQueue = new FilterDirtyQueue<>(nextNodesTupleLifecycle, tupleStateStoreIndex);
    }

    @Override
    public void insert(Tuple_ tuple) {
        if (tuple.getStore(tupleStateStoreIndex) != null) {
            throw new IllegalStateException("Impossible state: the tuple (" + tuple + ") was already inserted.");
        }
        if (testFiltering(tuple)) {
            dirtyTupleQueue.insertWithState(tuple, TupleState.CREATING);
        }
    }

    protected abstract boolean testFiltering(Tuple_ tuple);

    @Override
    public void update(Tuple_ tuple) {
        TupleState tupleState = tuple.getStore(tupleStateStoreIndex);
        if (tupleState == null) { // The tuple was never inserted because it did not pass the filter.
            insert(tuple);
        } else if (testFiltering(tuple)) {
            dirtyTupleQueue.insertWithState(tuple, TupleState.UPDATING);
        } else {
            retract(tuple);
        }
    }

    @Override
    public void retract(Tuple_ tuple) {
        TupleState tupleState = tuple.removeStore(tupleStateStoreIndex);
        if (tupleState == null) { // The tuple was never inserted because it did not pass the filter.
            return;
        }
        dirtyTupleQueue.insertWithState(tuple,
                tupleState.isDirty() ? TupleState.ABORTING : TupleState.DYING);
    }

    @Override
    public void calculateScore() {
        dirtyTupleQueue.calculateScore(this);
    }

}
