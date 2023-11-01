package ai.timefold.solver.core.impl.score.stream.bi;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.LongCounter;

public final class LongCountBiCollector<A, B> implements BiConstraintCollector<A, B, LongCounter, Long> {
    @Override
    public Supplier<LongCounter> supplier() {
        return LongCounter::new;
    }

    @Override
    public TriFunction<LongCounter, A, B, Runnable> accumulator() {
        return (counter, a, b) -> {
            counter.increment();
            return counter::decrement;
        };
    }

    @Override
    public Function<LongCounter, Long> finisher() {
        return LongCounter::result;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LongCountBiCollector;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
