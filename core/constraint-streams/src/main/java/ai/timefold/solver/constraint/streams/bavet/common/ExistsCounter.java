package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

public final class ExistsCounter<Tuple_ extends AbstractTuple>
        extends AbstractPropagationMetadataCarrier {

    final Tuple_ leftTuple;
    TupleState state = TupleState.DEAD; // It's the node's job to mark a new instance as CREATING.
    int countRight = 0;

    ExistsCounter(Tuple_ leftTuple) {
        this.leftTuple = leftTuple;
    }

    @Override
    public String toString() {
        return "Counter(" + leftTuple + ")";
    }

}
