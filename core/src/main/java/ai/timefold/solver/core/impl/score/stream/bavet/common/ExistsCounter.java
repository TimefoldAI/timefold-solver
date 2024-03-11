package ai.timefold.solver.core.impl.score.stream.bavet.common;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleState;

public final class ExistsCounter<Tuple_ extends AbstractTuple>
        extends AbstractPropagationMetadataCarrier<Tuple_> {

    final Tuple_ leftTuple;
    TupleState state = TupleState.DEAD; // It's the node's job to mark a new instance as CREATING.
    int countRight = 0;

    ExistsCounter(Tuple_ leftTuple) {
        this.leftTuple = leftTuple;
    }

    @Override
    public Tuple_ getTuple() {
        return leftTuple;
    }

    @Override
    public TupleState getState() {
        return state;
    }

    @Override
    public void setState(TupleState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "Counter(" + leftTuple + ")";
    }

}
