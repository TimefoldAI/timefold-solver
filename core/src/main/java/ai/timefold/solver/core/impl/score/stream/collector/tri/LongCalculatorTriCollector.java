package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LongCalculator;

import org.jspecify.annotations.NonNull;

abstract sealed class LongCalculatorTriCollector<A, B, C, Output_, State_, Calculator_ extends LongCalculator>
        implements TriConstraintCollector<A, B, C, State_, Output_> permits AverageTriCollector, SumTriCollector {
    private final ToLongTriFunction<? super A, ? super B, ? super C> mapper;

    public LongCalculatorTriCollector(ToLongTriFunction<? super A, ? super B, ? super C> mapper) {
        this.mapper = mapper;
    }

    protected abstract Calculator_ newCalculator(State_ state);

    @Override
    public @NonNull QuadFunction<State_, A, B, C, Runnable> accumulator() {
        return (state, a, b, c) -> {
            var calc = newCalculator(state);
            calc.insert(mapper.applyAsLong(a, b, c));
            return calc::retract;
        };
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (LongCalculatorTriCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
