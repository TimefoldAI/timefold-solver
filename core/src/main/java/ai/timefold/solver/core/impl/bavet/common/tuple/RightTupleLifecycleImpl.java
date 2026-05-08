package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.Objects;

final class RightTupleLifecycleImpl<Tuple_ extends Tuple>
        implements TupleLifecycle<Tuple_> {

    private final RightTupleLifecycle<Tuple_> rightTupleLifecycle;
    private boolean isActive;

    RightTupleLifecycleImpl(RightTupleLifecycle<Tuple_> rightTupleLifecycle) {
        Objects.requireNonNull(rightTupleLifecycle);
        this.rightTupleLifecycle = rightTupleLifecycle;
    }

    @Override
    public void initialize(boolean upstreamCanProduceTuples) {
        this.isActive = upstreamCanProduceTuples; // We're just delegating.
        rightTupleLifecycle.initializeRight(upstreamCanProduceTuples);
    }

    @Override
    public boolean isActive() {
        return isActive;
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

    public RightTupleLifecycle<Tuple_> rightTupleLifecycle() {
        return rightTupleLifecycle;
    }

    @Override
    public String toString() {
        return "right " + rightTupleLifecycle;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RightTupleLifecycleImpl<?> other
                && rightTupleLifecycle.equals(other.rightTupleLifecycle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rightTupleLifecycle);
    }

}
