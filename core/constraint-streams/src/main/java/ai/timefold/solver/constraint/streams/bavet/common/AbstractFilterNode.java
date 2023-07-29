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
 * its state is already {@link TupleState#OK} from the source node.
 * Therefore, in order not to have to create a clone of the tuple,
 * this node's local tuple state is stored in {@link AbstractTuple}'s state store as opposed to {@link AbstractTuple#state}.
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
        TupleState tupleState = tuple.getStore(tupleStateStoreIndex);
        if (tupleState != null && tupleState.isActive()) {
            throw new IllegalStateException("Impossible state: the tuple (" + tuple + ") was already inserted.");
        }
        if (testFiltering(tuple)) {
            dirtyTupleQueue.insertWithState(tuple, TupleState.CREATING);
        }
    }

    protected abstract boolean testFiltering(Tuple_ tuple);

    @Override
    public void update(Tuple_ tuple) {
        /*
         * The tuple state here is a local state for the purposes of the filter node.
         * It does not represent the state of the tuple as coming from the source node.
         * Therefore even locally dead tuple may be valid,
         * as dead in this context means the tuple went through calculateScore()
         * and was aborted/died locally in the filter node,
         * but not in the source node.
         */
        TupleState tupleState = tuple.getStore(tupleStateStoreIndex);
        if (tupleState == null) { // The tuple was never locally inserted because it did not pass the filter.
            insert(tuple);
        } else if (testFiltering(tuple)) {
            switch (tupleState) {
                case DEAD: // Already went through calculateScore(), this is a fresh update.
                case OK: // Already went through calculateScore(), this is a fresh update.
                    dirtyTupleQueue.insertWithState(tuple, TupleState.UPDATING);
                    break;
                case CREATING: // No need to update the tuple, it is being inserted.
                case UPDATING: // No need to update the tuple, as it is already being updated.
                    break;
                default:
                    throw new IllegalStateException("Impossible state: the tuple (" + tuple + ") was (" + tupleState + ").");
            }
        } else {
            retract(tuple);
        }
    }

    @Override
    public void retract(Tuple_ tuple) {
        TupleState tupleState = tuple.removeStore(tupleStateStoreIndex);
        if (tupleState == null) { // The tuple was never locally inserted because it did not pass the filter.
            return;
        } else if (!tupleState.isActive() && tupleState != TupleState.DEAD) {
            /*
             * Only active tuples may be retracted;
             * unless they are already locally dead,
             * which means a fresh retract from the source node.
             */
            throw new IllegalStateException("Impossible state: the tuple (" + tuple + ") is not active (" + tupleState + ").");
        }
        dirtyTupleQueue.insertWithState(tuple,
                tupleState == TupleState.CREATING ? TupleState.ABORTING : TupleState.DYING);
    }

    @Override
    public void calculateScore() {
        dirtyTupleQueue.calculateScore(this);
    }

}
