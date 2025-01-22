package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.Arrays;
import java.util.Objects;

record AggregatedTupleLifecycle<Tuple_ extends AbstractTuple>(TupleLifecycle<Tuple_>... lifecycles)
        implements
            TupleLifecycle<Tuple_> {

    @SafeVarargs
    public AggregatedTupleLifecycle {
        // Exists so that we have something to put the @SafeVarargs annotation on.
    }

    @Override
    public void insert(Tuple_ tuple) {
        for (var lifecycle : lifecycles) {
            lifecycle.insert(tuple);
        }
    }

    @Override
    public void update(Tuple_ tuple) {
        for (var lifecycle : lifecycles) {
            lifecycle.update(tuple);
        }
    }

    @Override
    public void retract(Tuple_ tuple) {
        for (var lifecycle : lifecycles) {
            lifecycle.retract(tuple);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AggregatedTupleLifecycle<?> that &&
                Objects.deepEquals(lifecycles, that.lifecycles);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(lifecycles);
    }

    @Override
    public String toString() {
        return "size = " + lifecycles.length;
    }
}
