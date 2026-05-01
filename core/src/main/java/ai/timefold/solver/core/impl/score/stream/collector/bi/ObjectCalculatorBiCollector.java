package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.ObjectCalculator;

import org.jspecify.annotations.NonNull;

abstract sealed class ObjectCalculatorBiCollector<A, B, Input_, Output_, State_, Calculator_ extends ObjectCalculator<Input_>>
        implements BiConstraintCollector<A, B, State_, Output_>
        permits AverageReferenceBiCollector, ConnectedRangesBiConstraintCollector, ConsecutiveSequencesBiConstraintCollector,
        CountDistinctBiCollector, SumReferenceBiCollector {
    protected final BiFunction<? super A, ? super B, ? extends Input_> mapper;

    ObjectCalculatorBiCollector(BiFunction<? super A, ? super B, ? extends Input_> mapper) {
        this.mapper = mapper;
    }

    protected abstract Calculator_ newCalculator(State_ state);

    @Override
    public @NonNull TriFunction<State_, A, B, Runnable> accumulator() {
        return (state, a, b) -> {
            var calc = newCalculator(state);
            calc.insert(mapper.apply(a, b));
            return calc::retract;
        };
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (ObjectCalculatorBiCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
