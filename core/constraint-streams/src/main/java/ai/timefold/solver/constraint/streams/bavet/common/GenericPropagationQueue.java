package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

public final class GenericPropagationQueue<Tuple_ extends AbstractTuple>
        extends AbstractStaticPropagationQueue<Tuple_> {

    public GenericPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        super(nextNodesTupleLifecycle);
    }

    @Override
    protected TupleState extractState(Tuple_ tuple) {
        return tuple.state;
    }

    @Override
    protected void changeState(Tuple_ tuple, TupleState state) {
        tuple.state = state;
    }

}
