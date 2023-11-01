package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.IntCounter;

public final class IntCountTriCollector<A, B, C> implements TriConstraintCollector<A, B, C, IntCounter, Integer> {
    @Override
    public Supplier<IntCounter> supplier() {
        return IntCounter::new;
    }

    @Override
    public QuadFunction<IntCounter, A, B, C, Runnable> accumulator() {
        return (counter, a, b, c) -> {
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
        return o instanceof IntCountTriCollector;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
