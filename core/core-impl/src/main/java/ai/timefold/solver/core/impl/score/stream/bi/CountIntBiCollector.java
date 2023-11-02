package ai.timefold.solver.core.impl.score.stream.bi;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.IntCounter;

public final class CountIntBiCollector<A, B> implements BiConstraintCollector<A, B, IntCounter, Integer> {
    CountIntBiCollector() {
    }

    @Override
    public Supplier<IntCounter> supplier() {
        return IntCounter::new;
    }

    @Override
    public TriFunction<IntCounter, A, B, Runnable> accumulator() {
        return (counter, a, b) -> {
            counter.increment();
            return counter::decrement;
        };
    }

    @Override
    public Function<IntCounter, Integer> finisher() {
        return IntCounter::result;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CountIntBiCollector;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
