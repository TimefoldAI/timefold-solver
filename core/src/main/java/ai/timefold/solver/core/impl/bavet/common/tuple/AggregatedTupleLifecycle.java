package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.Arrays;

public final class AggregatedTupleLifecycle<Tuple_ extends Tuple>
        implements TupleLifecycle<Tuple_> {

    private boolean upstreamCanProduceTuples;
    private TupleLifecycle<Tuple_>[] downstream;
    private boolean downstreamFinal = false;

    @SafeVarargs
    public AggregatedTupleLifecycle(TupleLifecycle<Tuple_>... downstream) {
        this.downstream = downstream;
    }

    @Override
    public void afterAllFactsInserted(boolean upstreamCanProduceTuples) {
        for (var lifecycle : downstream) { // First initialize all downstream lifecycles.
            lifecycle.afterAllFactsInserted(upstreamCanProduceTuples);
        }
        this.upstreamCanProduceTuples = upstreamCanProduceTuples;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isActive() {
        if (downstreamFinal) {
            return downstream.length > 0;
        }
        if (upstreamCanProduceTuples) {
            // Iterating a list in update() was measurably slower in micro benchmarks, so we deal with arrays.
            downstream = Arrays.stream(downstream)
                    .distinct()
                    .filter(TupleLifecycle::isActive)
                    .toArray(TupleLifecycle[]::new);
        } else {
            // No upstream facts, so downstream lifecycles will never be active.
            downstream = new TupleLifecycle[0];
        }
        downstreamFinal = true;
        return downstream.length > 0;
    }

    @Override
    public void insert(Tuple_ tuple) {
        for (var lifecycle : downstream) {
            lifecycle.insert(tuple);
        }
    }

    @Override
    public void update(Tuple_ tuple) {
        for (var lifecycle : downstream) {
            lifecycle.update(tuple);
        }
    }

    @Override
    public void retract(Tuple_ tuple) {
        for (var lifecycle : downstream) {
            lifecycle.retract(tuple);
        }
    }

    /**
     * Users must not modify this array. (Defensive copy avoided for performance reasons.)
     *
     * @return active downstream lifecycles
     */
    public TupleLifecycle<Tuple_>[] downstream() {
        return downstream;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AggregatedTupleLifecycle<?> that &&
                Arrays.deepEquals(downstream, that.downstream);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(downstream);
    }

    @Override
    public String toString() {
        return "size = " + downstream.length;
    }
}
