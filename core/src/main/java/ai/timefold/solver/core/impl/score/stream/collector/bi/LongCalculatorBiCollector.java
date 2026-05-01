package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LongCalculator;

import org.jspecify.annotations.NonNull;

abstract sealed class LongCalculatorBiCollector<A, B, Output_, State_, Calculator_ extends LongCalculator>
        implements BiConstraintCollector<A, B, State_, Output_> permits AverageBiCollector, SumBiCollector {
    protected final ToLongBiFunction<? super A, ? super B> mapper;

    LongCalculatorBiCollector(ToLongBiFunction<? super A, ? super B> mapper) {
        this.mapper = mapper;
    }

    protected abstract Calculator_ newCalculator(State_ state);

    @Override
    public @NonNull TriFunction<State_, A, B, Runnable> accumulator() {
        return (state, a, b) -> {
            var calc = newCalculator(state);
            calc.insert(mapper.applyAsLong(a, b));
            return calc::retract;
        };
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (LongCalculatorBiCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
