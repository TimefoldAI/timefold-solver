package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LongCalculator;

abstract sealed class LongCalculatorTriCollector<A, B, C, Output_, Calculator_ extends LongCalculator<Output_>>
        implements TriConstraintCollector<A, B, C, Calculator_, Output_> permits AverageLongTriCollector, SumLongTriCollector {
    private final ToLongTriFunction<? super A, ? super B, ? super C> mapper;

    public LongCalculatorTriCollector(ToLongTriFunction<? super A, ? super B, ? super C> mapper) {
        this.mapper = mapper;
    }

    @Override
    public QuadFunction<Calculator_, A, B, C, Runnable> accumulator() {
        return (calculator, a, b, c) -> {
            final long mapped = mapper.applyAsLong(a, b, c);
            calculator.insert(mapped);
            return () -> calculator.retract(mapped);
        };
    }

    @Override
    public Function<Calculator_, Output_> finisher() {
        return LongCalculator::result;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (LongCalculatorTriCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
