package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

public final class GenericDirtyQueue<Tuple_ extends AbstractTuple> extends AbstractDirtyQueue<Tuple_, Tuple_> {

    public GenericDirtyQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        super(nextNodesTupleLifecycle);
    }

    @Override
    protected Tuple_ extractTuple(Tuple_ tuple) {
        return tuple;
    }

    @Override
    protected TupleState extractState(Tuple_ tuple) {
        return tuple.state;
    }

    @Override
    public void changeState(Tuple_ tuple, TupleState state) {
        tuple.state = state;
    }
}
