package ai.timefold.solver.constraint.streams.bavet.common;

import static ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState.DEAD;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

public final class ExistsCounter<Tuple_ extends AbstractTuple> {

    final Tuple_ leftTuple;
    TupleState state = DEAD;
    int countRight = 0;

    ExistsCounter(Tuple_ leftTuple) {
        this.leftTuple = leftTuple;
    }

    @Override
    public String toString() {
        return "Counter(" + leftTuple + ")";
    }

}
