package ai.timefold.solver.core.impl.bavet.common;

import java.util.ArrayList;

import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.bavet.common.tuple.indictment.IndictmentSource;

public final class ExistsCounter<Tuple_ extends Tuple>
        extends AbstractPropagationMetadataCarrier<Tuple_> {

    final Tuple_ leftTuple;
    final Tuple_ outTuple;
    TupleState state = TupleState.DEAD; // It's the node's job to mark a new instance as CREATING.
    int countRight = 0;

    ExistsCounter(Tuple_ leftTuple) {
        this.leftTuple = leftTuple;
        if (leftTuple.getIndictmentSource() == IndictmentSource.DISABLED) {
            outTuple = leftTuple;
        } else {
            outTuple = Tuple.copyOf(leftTuple);
            outTuple.setIndictmentSource(new IndictmentSource.IndictmentSourceWithSupport(leftTuple.getIndictmentSource(),
                    new ArrayList<>()));
        }
    }

    @Override
    public Tuple_ getTuple() {
        return outTuple;
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
        return "Counter(%s)".formatted(leftTuple);
    }

}
