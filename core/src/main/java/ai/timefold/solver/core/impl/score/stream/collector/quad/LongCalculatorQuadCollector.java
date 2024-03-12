package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LongCalculator;

abstract sealed class LongCalculatorQuadCollector<A, B, C, D, Output_, Calculator_ extends LongCalculator<Output_>>
        implements QuadConstraintCollector<A, B, C, D, Calculator_, Output_>
        permits AverageLongQuadCollector, SumLongQuadCollector {
    private final ToLongQuadFunction<? super A, ? super B, ? super C, ? super D> mapper;

    public LongCalculatorQuadCollector(ToLongQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
        this.mapper = mapper;
    }

    @Override
    public PentaFunction<Calculator_, A, B, C, D, Runnable> accumulator() {
        return (calculator, a, b, c, d) -> {
            final long mapped = mapper.applyAsLong(a, b, c, d);
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
        var that = (LongCalculatorQuadCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
