package ai.timefold.solver.constraint.streams.bavet.uni;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractStaticPropagationQueue;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

final class ForEachPropagationQueue<Tuple_ extends AbstractTuple> extends AbstractStaticPropagationQueue<Tuple_, Tuple_> {

    public ForEachPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
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
    protected void changeState(Tuple_ tuple, TupleState state) {
        tuple.state = state;
    }

}
