package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LongCounter;

final class CountLongQuadCollector<A, B, C, D> implements QuadConstraintCollector<A, B, C, D, LongCounter, Long> {
    private final static CountLongQuadCollector<?, ?, ?, ?> INSTANCE = new CountLongQuadCollector<>();

    private CountLongQuadCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A, B, C, D> CountLongQuadCollector<A, B, C, D> getInstance() {
        return (CountLongQuadCollector<A, B, C, D>) INSTANCE;
    }

    @Override
    public Supplier<LongCounter> supplier() {
        return LongCounter::new;
    }

    @Override
    public PentaFunction<LongCounter, A, B, C, D, Runnable> accumulator() {
        return (counter, a, b, c, d) -> {
            counter.increment();
            return counter::decrement;
        };
    }

    @Override
    public Function<LongCounter, Long> finisher() {
        return LongCounter::result;
    }
}
