package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class LeftBridgeTupleLifecycle<Tuple_ extends Tuple>
        implements TupleLifecycle<Tuple_> {

    private final LeftTupleLifecycle<Tuple_> leftTupleLifecycle;
    private boolean isActive;

    LeftBridgeTupleLifecycle(LeftTupleLifecycle<Tuple_> leftTupleLifecycle) {
        this.leftTupleLifecycle = Objects.requireNonNull(leftTupleLifecycle);
    }

    @Override
    public void afterAllFactsInserted(boolean upstreamCanProduceTuples) {
        this.isActive = upstreamCanProduceTuples; // We're just delegating.
        leftTupleLifecycle.afterAllFactsInsertedLeft(upstreamCanProduceTuples);
    }

    @Override
    public boolean isActive() {
        return isActive && leftTupleLifecycle.isActive();
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

    public LeftTupleLifecycle<Tuple_> leftTupleLifecycle() {
        return leftTupleLifecycle;
    }

    @Override
    public String toString() {
        return "left %s".formatted(leftTupleLifecycle);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LeftBridgeTupleLifecycle<?> other
                && Objects.equals(this.leftTupleLifecycle, other.leftTupleLifecycle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftTupleLifecycle);
    }

}
