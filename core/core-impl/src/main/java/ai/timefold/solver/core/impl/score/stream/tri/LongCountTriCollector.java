package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.LongCounter;

public final class LongCountTriCollector<A, B, C> implements TriConstraintCollector<A, B, C, LongCounter, Long> {
    @Override
    public Supplier<LongCounter> supplier() {
        return LongCounter::new;
    }

    @Override
    public QuadFunction<LongCounter, A, B, C, Runnable> accumulator() {
        return (counter, a, b, c) -> {
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
        return o instanceof LongCountTriCollector;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
