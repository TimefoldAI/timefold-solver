package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.Objects;
import java.util.function.Predicate;

public final class ConditionalTupleLifecycle<Tuple_ extends Tuple>
        implements TupleLifecycle<Tuple_> {

    private final TupleLifecycle<Tuple_> downstreamLifecycle;
    private final TuplePredicate<Tuple_> predicate;
    private boolean isActive;

    public ConditionalTupleLifecycle(TupleLifecycle<Tuple_> downstreamLifecycle, TuplePredicate<Tuple_> predicate) {
        Objects.requireNonNull(downstreamLifecycle);
        Objects.requireNonNull(predicate);
        this.downstreamLifecycle = downstreamLifecycle;
        this.predicate = predicate;
    }

    @Override
    public void initialize(boolean upstreamCanProduceTuples) {
        // It is possible the predicate will always filter everything out, but we cannot know that for certain.
        // We must pass the upstream information downstream, and be active if upstream can send anything to us.
        this.isActive = upstreamCanProduceTuples;
        downstreamLifecycle.initialize(upstreamCanProduceTuples);
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void insert(Tuple_ tuple) {
        if (predicate.test(tuple)) {
            downstreamLifecycle.insert(tuple);
        }
    }

    @Override
    public void update(Tuple_ tuple) {
        if (predicate.test(tuple)) {
            downstreamLifecycle.update(tuple);
        } else {
            downstreamLifecycle.retract(tuple);
        }
    }

    @Override
    public void retract(Tuple_ tuple) {
        downstreamLifecycle.retract(tuple);
    }

    @Override
    public String toString() {
        return "Conditional %s".formatted(downstreamLifecycle);
    }

    public TuplePredicate<Tuple_> predicate() {
        return predicate;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ConditionalTupleLifecycle<?> other
                && Objects.equals(this.downstreamLifecycle, other.downstreamLifecycle)
                && Objects.equals(this.predicate, other.predicate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(downstreamLifecycle, predicate);
    }

    @FunctionalInterface
    interface TuplePredicate<Tuple_ extends Tuple> extends Predicate<Tuple_> {
    }

}
