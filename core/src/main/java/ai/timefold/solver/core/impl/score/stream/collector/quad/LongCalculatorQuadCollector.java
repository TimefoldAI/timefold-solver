package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LongCalculator;

import org.jspecify.annotations.NonNull;

abstract sealed class LongCalculatorQuadCollector<A, B, C, D, Output_, State_, Calculator_ extends LongCalculator>
        implements QuadConstraintCollector<A, B, C, D, State_, Output_>
        permits AverageQuadCollector, SumQuadCollector {
    private final ToLongQuadFunction<? super A, ? super B, ? super C, ? super D> mapper;

    public LongCalculatorQuadCollector(ToLongQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
        this.mapper = mapper;
    }

    protected abstract Calculator_ newCalculator(State_ state);

    @Override
    public @NonNull PentaFunction<State_, A, B, C, D, Runnable> accumulator() {
        return (state, a, b, c, d) -> {
            var calc = newCalculator(state);
            calc.insert(mapper.applyAsLong(a, b, c, d));
            return calc::retract;
        };
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (LongCalculatorQuadCollector<?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
