package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.IntCounter;

final class CountIntBiCollector<A, B> implements BiConstraintCollector<A, B, IntCounter, Integer> {
    private final static CountIntBiCollector<?, ?> INSTANCE = new CountIntBiCollector<>();

    private CountIntBiCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A, B> CountIntBiCollector<A, B> getInstance() {
        return (CountIntBiCollector<A, B>) INSTANCE;
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
}
