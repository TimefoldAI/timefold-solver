package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class AggregatedTupleLifecycle<Tuple_ extends Tuple>
        implements TupleLifecycle<Tuple_> {

    private final List<TupleLifecycle<Tuple_>> downstream;
    private boolean downstreamFinal = false;

    @SafeVarargs
    public AggregatedTupleLifecycle(TupleLifecycle<Tuple_>... downstream) {
        this.downstream = Arrays.stream(downstream)
                .collect(Collectors.toList());
    }

    @Override
    public void initialize(boolean upstreamCanProduceTuples) {
        for (var lifecycle : downstream) { // First initialize all downstream lifecycles.
            lifecycle.initialize(upstreamCanProduceTuples);
        }
    }

    @Override
    public boolean isActive() {
        if (!downstreamFinal) {
            downstream.removeIf(lifecycle -> !lifecycle.isActive());
            downstreamFinal = true;
        }
        return !downstream.isEmpty();
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
                downstream.equals(that.downstream);
    }

    public List<TupleLifecycle<Tuple_>> downstream() {
        return downstream;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(downstream);
    }

    @Override
    public String toString() {
        return "size = " + downstream.size();
    }
}
