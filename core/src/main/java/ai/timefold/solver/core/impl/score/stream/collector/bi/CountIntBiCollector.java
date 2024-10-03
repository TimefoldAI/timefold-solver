package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.IntCounter;

import org.jspecify.annotations.NonNull;

final class CountIntBiCollector<A, B> implements BiConstraintCollector<A, B, IntCounter, Integer> {
    private final static CountIntBiCollector<?, ?> INSTANCE = new CountIntBiCollector<>();

    private CountIntBiCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A, B> CountIntBiCollector<A, B> getInstance() {
        return (CountIntBiCollector<A, B>) INSTANCE;
    }

    @Override
    public @NonNull Supplier<IntCounter> supplier() {
        return IntCounter::new;
    }

    @Override
    public @NonNull TriFunction<IntCounter, A, B, Runnable> accumulator() {
        return (counter, a, b) -> {
            counter.increment();
            return counter::decrement;
        };
    }

    @Override
    public @NonNull Function<IntCounter, Integer> finisher() {
        return IntCounter::result;
    }
}
