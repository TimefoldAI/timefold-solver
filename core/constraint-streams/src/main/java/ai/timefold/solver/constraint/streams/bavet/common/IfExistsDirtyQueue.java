package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

final class IfExistsDirtyQueue<Tuple_ extends AbstractTuple> extends AbstractDirtyQueue<ExistsCounter<Tuple_>, Tuple_> {

    public IfExistsDirtyQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        super(nextNodesTupleLifecycle);
    }

    @Override
    protected Tuple_ extractTuple(ExistsCounter<Tuple_> tupleExistsCounter) {
        return tupleExistsCounter.leftTuple;
    }

    @Override
    protected TupleState extractState(ExistsCounter<Tuple_> tupleExistsCounter) {
        return tupleExistsCounter.state;
    }

    @Override
    public void changeState(ExistsCounter<Tuple_> tupleExistsCounter, TupleState state) {
        tupleExistsCounter.state = state;
    }

}
