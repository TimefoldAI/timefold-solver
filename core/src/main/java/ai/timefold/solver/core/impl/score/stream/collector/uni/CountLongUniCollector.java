package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LongCounter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class CountLongUniCollector<A> implements UniConstraintCollector<A, LongCounter, Long> {
    private final static CountLongUniCollector<?> INSTANCE = new CountLongUniCollector<>();

    private CountLongUniCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A> CountLongUniCollector<A> getInstance() {
        return (CountLongUniCollector<A>) INSTANCE;
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
