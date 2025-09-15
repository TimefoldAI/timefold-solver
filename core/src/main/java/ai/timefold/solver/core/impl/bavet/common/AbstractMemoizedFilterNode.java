package ai.timefold.solver.core.impl.bavet.common;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

import org.jspecify.annotations.NullMarked;

/**
 * Implements a filter node which only checks the predicate at tuple insertion.
 * Updates and retracts to that tuple are only propagated if the tuple originally passed the filter.
 * The predicate is not re-evaluated on update or retract.
 */
@NullMarked
public abstract class AbstractMemoizedFilterNode<Tuple_ extends AbstractTuple>
        extends AbstractNode
        implements TupleLifecycle<Tuple_> {

    private final int inputStoreIndex;
    private final StaticPropagationQueue<Tuple_> propagationQueue;

    protected AbstractMemoizedFilterNode(int inputStoreIndex, TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        this.inputStoreIndex = inputStoreIndex;
        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
    }

    protected abstract boolean testFiltering(Tuple_ tuple);

    @Override
    public final void insert(Tuple_ tuple) {
        var passedFilter = testFiltering(tuple);
        tuple.setStore(inputStoreIndex, passedFilter);
        if (passedFilter) {
            propagationQueue.insert(tuple);
        }
    }

    @Override
    public final void update(Tuple_ tuple) {
        var passedFilter = tuple.getStore(inputStoreIndex);
        if (passedFilter == null) {
            // No fail fast if null because we don't track which tuples made it through the regular filter predicate(s)
            insert(tuple);
        } else if (Objects.equals(passedFilter, Boolean.TRUE)) {
            propagationQueue.update(tuple);
        }
    }

    @Override
    public final void retract(Tuple_ tuple) {
        var passedFilter = tuple.removeStore(inputStoreIndex);
        if (Objects.equals(passedFilter, Boolean.TRUE)) {
            propagationQueue.retract(tuple);
        }
    }

    @Override
    public final Propagator getPropagator() {
        return propagationQueue;
    }

}
