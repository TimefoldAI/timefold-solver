package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LongCounter;

final class CountLongBiCollector<A, B> implements BiConstraintCollector<A, B, LongCounter, Long> {
    private final static CountLongBiCollector<?, ?> INSTANCE = new CountLongBiCollector<>();

    private CountLongBiCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A, B> CountLongBiCollector<A, B> getInstance() {
        return (CountLongBiCollector<A, B>) INSTANCE;
    }

    @Override
    public Supplier<LongCounter> supplier() {
        return LongCounter::new;
    }

    @Override
    public TriFunction<LongCounter, A, B, Runnable> accumulator() {
        return (counter, a, b) -> {
            counter.increment();
            return counter::decrement;
        };
    }

    @Override
    public Function<LongCounter, Long> finisher() {
        return LongCounter::result;
    }
}
