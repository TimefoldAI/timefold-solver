package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LongCounter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class CountUniCollector<A> implements UniConstraintCollector<A, LongCounter, Long> {
    private static final CountUniCollector<?> INSTANCE = new CountUniCollector<>();

    private CountUniCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A> CountUniCollector<A> getInstance() {
        return (CountUniCollector<A>) INSTANCE;
    }

    @Override
    public @NonNull Supplier<LongCounter> supplier() {
        return LongCounter::new;
    }

    @Override
    public @NonNull BiFunction<LongCounter, A, Runnable> accumulator() {
        return (counter, a) -> {
            counter.increment();
            return counter::decrement;
        };
    }

    @Override
    public @Nullable Function<LongCounter, Long> finisher() {
        return LongCounter::result;
    }
}
