package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.ObjectCalculator;

import org.jspecify.annotations.NonNull;

abstract sealed class ObjectCalculatorTriCollector<A, B, C, Input_, Output_, State_, Calculator_ extends ObjectCalculator<Input_>>
        implements TriConstraintCollector<A, B, C, State_, Output_>
        permits AverageReferenceTriCollector, ConnectedRangesTriConstraintCollector, ConsecutiveSequencesTriConstraintCollector,
        CountDistinctTriCollector, SumReferenceTriCollector {
    protected final TriFunction<? super A, ? super B, ? super C, ? extends Input_> mapper;

    public ObjectCalculatorTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Input_> mapper) {
        this.mapper = mapper;
    }

    protected abstract Calculator_ newCalculator(State_ state);

    @Override
    public @NonNull QuadFunction<State_, A, B, C, Runnable> accumulator() {
        return (state, a, b, c) -> {
            var calc = newCalculator(state);
            calc.insert(mapper.apply(a, b, c));
            return calc::retract;
        };
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (ObjectCalculatorTriCollector<?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
