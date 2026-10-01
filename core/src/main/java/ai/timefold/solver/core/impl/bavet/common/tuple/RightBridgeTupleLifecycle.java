package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class RightBridgeTupleLifecycle<Tuple_ extends Tuple>
        implements TupleLifecycle<Tuple_> {

    private final RightTupleLifecycle<Tuple_> rightTupleLifecycle;
    private boolean isActive;

    RightBridgeTupleLifecycle(RightTupleLifecycle<Tuple_> rightTupleLifecycle) {
        this.rightTupleLifecycle = Objects.requireNonNull(rightTupleLifecycle);
    }

    @Override
    public void afterAllFactsInserted(boolean upstreamCanProduceTuples) {
        this.isActive = upstreamCanProduceTuples; // We're just delegating.
        rightTupleLifecycle.afterAllFactsInsertedRight(upstreamCanProduceTuples);
    }

    @Override
    public boolean isActive() {
        return isActive && rightTupleLifecycle.isActive();
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
        return "right %s".formatted(rightTupleLifecycle);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RightBridgeTupleLifecycle<?> other
                && rightTupleLifecycle.equals(other.rightTupleLifecycle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rightTupleLifecycle);
    }

}
