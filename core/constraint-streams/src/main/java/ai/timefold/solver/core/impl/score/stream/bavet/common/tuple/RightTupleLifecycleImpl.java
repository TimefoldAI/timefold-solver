package ai.timefold.solver.core.impl.score.stream.bavet.common.tuple;

import java.util.Objects;

final class RightTupleLifecycleImpl<Tuple_ extends AbstractTuple>
        implements TupleLifecycle<Tuple_> {

    private final RightTupleLifecycle<Tuple_> rightTupleLifecycle;

    RightTupleLifecycleImpl(RightTupleLifecycle<Tuple_> rightTupleLifecycle) {
        this.rightTupleLifecycle = Objects.requireNonNull(rightTupleLifecycle);
    }

    @Override
    public void insert(Tuple_ tuple) {
        rightTupleLifecycle.insertRight(tuple);
    }

    @Override
    public void update(Tuple_ tuple) {
        rightTupleLifecycle.updateRight(tuple);
    }

    @Override
    public void retract(Tuple_ tuple) {
        rightTupleLifecycle.retractRight(tuple);
    }

    @Override
    public String toString() {
        return "right " + rightTupleLifecycle;
    }

}
