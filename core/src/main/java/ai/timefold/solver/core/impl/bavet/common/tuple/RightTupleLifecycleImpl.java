package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.Objects;

record RightTupleLifecycleImpl<Tuple_ extends AbstractTuple>(RightTupleLifecycle<Tuple_> rightTupleLifecycle)
        implements
            TupleLifecycle<Tuple_> {

    RightTupleLifecycleImpl {
        Objects.requireNonNull(rightTupleLifecycle);
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
