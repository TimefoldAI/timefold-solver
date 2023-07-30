package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

final class FilterPropagationQueue<Tuple_ extends AbstractTuple> extends AbstractStaticPropagationQueue<Tuple_, Tuple_> {

    private final int tupleStateStoreIndex;

    public FilterPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int tupleStateStoreIndex) {
        super(nextNodesTupleLifecycle);
        this.tupleStateStoreIndex = tupleStateStoreIndex;
    }

    @Override
    protected Tuple_ extractTuple(Tuple_ tuple) {
        return tuple;
    }

    @Override
    protected TupleState extractState(Tuple_ tuple) {
        return tuple.getStore(tupleStateStoreIndex);
    }

    @Override
    protected void changeState(Tuple_ tuple, TupleState state) {
        tuple.setStore(tupleStateStoreIndex, state);
    }

}
