package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LongCounter;

import org.jspecify.annotations.NonNull;

final class CountTriCollector<A, B, C> implements TriConstraintCollector<A, B, C, LongCounter, Long> {
    private final static CountTriCollector<?, ?, ?> INSTANCE = new CountTriCollector<>();

    private CountTriCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A, B, C> CountTriCollector<A, B, C> getInstance() {
        return (CountTriCollector<A, B, C>) INSTANCE;
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
