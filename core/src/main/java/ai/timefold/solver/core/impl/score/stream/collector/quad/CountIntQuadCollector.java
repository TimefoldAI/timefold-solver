package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.IntCounter;

final class CountIntQuadCollector<A, B, C, D> implements QuadConstraintCollector<A, B, C, D, IntCounter, Integer> {
    private final static CountIntQuadCollector<?, ?, ?, ?> INSTANCE = new CountIntQuadCollector<>();

    private CountIntQuadCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A, B, C, D> CountIntQuadCollector<A, B, C, D> getInstance() {
        return (CountIntQuadCollector<A, B, C, D>) INSTANCE;
    }

    @Override
    public Supplier<IntCounter> supplier() {
        return IntCounter::new;
    }

    @Override
    public PentaFunction<IntCounter, A, B, C, D, Runnable> accumulator() {
        return (counter, a, b, c, d) -> {
            counter.increment();
            return counter::decrement;
        };
    }

    @Override
    public Function<IntCounter, Integer> finisher() {
        return IntCounter::result;
    }
}
