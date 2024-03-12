package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.ToIntQuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.IntCalculator;

abstract sealed class IntCalculatorQuadCollector<A, B, C, D, Output_, Calculator_ extends IntCalculator<Output_>>
        implements QuadConstraintCollector<A, B, C, D, Calculator_, Output_>
        permits AverageIntQuadCollector, SumIntQuadCollector {
    private final ToIntQuadFunction<? super A, ? super B, ? super C, ? super D> mapper;

    public IntCalculatorQuadCollector(ToIntQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
        this.mapper = mapper;
    }

    @Override
    public PentaFunction<Calculator_, A, B, C, D, Runnable> accumulator() {
        return (calculator, a, b, c, d) -> {
            final int mapped = mapper.applyAsInt(a, b, c, d);
            calculator.insert(mapped);
            return () -> calculator.retract(mapped);
        };
    }

    @Override
    public Function<Calculator_, Output_> finisher() {
        return IntCalculator::result;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (IntCalculatorQuadCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
