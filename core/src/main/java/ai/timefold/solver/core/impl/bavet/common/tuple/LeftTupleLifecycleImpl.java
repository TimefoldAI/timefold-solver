package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.Objects;

record LeftTupleLifecycleImpl<Tuple_ extends AbstractTuple>(LeftTupleLifecycle<Tuple_> leftTupleLifecycle)
        implements
            TupleLifecycle<Tuple_> {

    LeftTupleLifecycleImpl {
        Objects.requireNonNull(leftTupleLifecycle);
    }

    @Override
    public void insert(Tuple_ tuple) {
        leftTupleLifecycle.insertLeft(tuple);
    }

    @Override
    public void update(Tuple_ tuple) {
        leftTupleLifecycle.updateLeft(tuple);
    }

    @Override
    public void retract(Tuple_ tuple) {
        leftTupleLifecycle.retractLeft(tuple);
    }

    @Override
    public String toString() {
        return "left " + leftTupleLifecycle;
    }

}
