package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.ObjectCalculator;

import org.jspecify.annotations.NonNull;

abstract sealed class ObjectCalculatorQuadCollector<A, B, C, D, Input_, Output_, State_, Calculator_ extends ObjectCalculator<Input_>>
        implements QuadConstraintCollector<A, B, C, D, State_, Output_>
        permits AverageReferenceQuadCollector, ConnectedRangesQuadConstraintCollector,
        ConsecutiveSequencesQuadConstraintCollector, CountDistinctQuadCollector,
        SumReferenceQuadCollector {

    protected final QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Input_> mapper;

    public ObjectCalculatorQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Input_> mapper) {
        this.mapper = mapper;
    }

    protected abstract Calculator_ newCalculator(State_ state);

    @Override
    public @NonNull PentaFunction<State_, A, B, C, D, Runnable> accumulator() {
        return (state, a, b, c, d) -> {
            var calc = newCalculator(state);
            calc.insert(mapper.apply(a, b, c, d));
            return calc::retract;
        };
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (ObjectCalculatorQuadCollector<?, ?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
