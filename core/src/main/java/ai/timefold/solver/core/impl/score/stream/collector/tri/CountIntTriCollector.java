package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.IntCounter;

final class CountIntTriCollector<A, B, C> implements TriConstraintCollector<A, B, C, IntCounter, Integer> {
    private final static CountIntTriCollector<?, ?, ?> INSTANCE = new CountIntTriCollector<>();

    private CountIntTriCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A, B, C> CountIntTriCollector<A, B, C> getInstance() {
        return (CountIntTriCollector<A, B, C>) INSTANCE;
    }

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
}
