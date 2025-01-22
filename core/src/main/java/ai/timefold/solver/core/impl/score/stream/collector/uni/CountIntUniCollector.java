package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.IntCounter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class CountIntUniCollector<A> implements UniConstraintCollector<A, IntCounter, Integer> {
    private final static CountIntUniCollector<?> INSTANCE = new CountIntUniCollector<>();

    private CountIntUniCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A> CountIntUniCollector<A> getInstance() {
        return (CountIntUniCollector<A>) INSTANCE;
    }

    @Override
    public @NonNull Supplier<IntCounter> supplier() {
        return IntCounter::new;
    }

    @Override
    public @NonNull BiFunction<IntCounter, A, Runnable> accumulator() {
        return (counter, a) -> {
            counter.increment();
            return counter::decrement;
        };
    }

    @Override
    public @Nullable Function<IntCounter, Integer> finisher() {
        return IntCounter::result;
    }
}
