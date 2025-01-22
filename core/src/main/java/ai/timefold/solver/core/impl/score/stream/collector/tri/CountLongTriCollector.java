package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LongCounter;

import org.jspecify.annotations.NonNull;

final class CountLongTriCollector<A, B, C> implements TriConstraintCollector<A, B, C, LongCounter, Long> {
    private final static CountLongTriCollector<?, ?, ?> INSTANCE = new CountLongTriCollector<>();

    private CountLongTriCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A, B, C> CountLongTriCollector<A, B, C> getInstance() {
        return (CountLongTriCollector<A, B, C>) INSTANCE;
    }

    @Override
    public @NonNull Supplier<LongCounter> supplier() {
        return LongCounter::new;
    }

    @Override
    public @NonNull QuadFunction<LongCounter, A, B, C, Runnable> accumulator() {
        return (counter, a, b, c) -> {
            counter.increment();
            return counter::decrement;
        };
    }

    @Override
    public @NonNull Function<LongCounter, Long> finisher() {
        return LongCounter::result;
    }
}
