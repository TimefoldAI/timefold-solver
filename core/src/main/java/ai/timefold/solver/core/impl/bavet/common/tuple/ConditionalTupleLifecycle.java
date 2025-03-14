package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.Objects;
import java.util.function.Predicate;

public record ConditionalTupleLifecycle<Tuple_ extends AbstractTuple>(TupleLifecycle<Tuple_> downstreamLifecycle,
        TuplePredicate<Tuple_> predicate)
        implements
            TupleLifecycle<Tuple_> {

    public ConditionalTupleLifecycle {
        Objects.requireNonNull(downstreamLifecycle);
        Objects.requireNonNull(predicate);
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

    @FunctionalInterface
    interface TuplePredicate<Tuple_ extends AbstractTuple> extends Predicate<Tuple_> {
    }

}
