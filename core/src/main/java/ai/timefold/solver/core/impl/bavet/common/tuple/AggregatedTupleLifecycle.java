package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.Arrays;
import java.util.Objects;

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
        if (upstreamCanProduceTuples) {
            if (!downstreamFinal) {
                // Iterating a list in update() was measurably slower in micro benchmarks, so we deal with arrays.
                downstream = Arrays.stream(downstream)
                        .filter(TupleLifecycle::isActive)
                        .toArray(TupleLifecycle[]::new);
                downstreamFinal = true;
            }
        } else {
            downstream = new TupleLifecycle[0];
            return false;
        }
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

    @Override
    public boolean equals(Object o) {
        return o instanceof AggregatedTupleLifecycle<?> that &&
                Arrays.deepEquals(downstream, that.downstream);
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
    public int hashCode() {
        return Objects.hashCode(downstream);
    }

    @Override
    public String toString() {
        return "size = " + downstream.length;
    }
}
