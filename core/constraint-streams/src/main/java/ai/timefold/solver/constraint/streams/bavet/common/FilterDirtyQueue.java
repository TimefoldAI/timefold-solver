package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

final class FilterDirtyQueue<Tuple_ extends AbstractTuple> extends AbstractDirtyQueue<Tuple_, Tuple_> {

    private final int tupleStateStoreIndex;

    public FilterDirtyQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int tupleStateStoreIndex) {
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
    public void changeState(Tuple_ tuple, TupleState state) {
        tuple.setStore(tupleStateStoreIndex, state);
    }
}
