package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.ObjectCalculator;

abstract sealed class ObjectCalculatorQuadCollector<A, B, C, D, Input_, Output_, Mapped_, Calculator_ extends ObjectCalculator<Input_, Output_, Mapped_>>
        implements QuadConstraintCollector<A, B, C, D, Calculator_, Output_>
        permits AverageReferenceQuadCollector, ConnectedRangesQuadConstraintCollector,
        ConsecutiveSequencesQuadConstraintCollector, CountDistinctIntQuadCollector, CountDistinctLongQuadCollector,
        SumReferenceQuadCollector {

    protected final QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Input_> mapper;

    public ObjectCalculatorQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Input_> mapper) {
        this.mapper = mapper;
    }

    @Override
    public PentaFunction<Calculator_, A, B, C, D, Runnable> accumulator() {
        return (calculator, a, b, c, d) -> {
            final var mapped = mapper.apply(a, b, c, d);
            final var saved = calculator.insert(mapped);
            return () -> calculator.retract(saved);
        };
    }

    @Override
    public Function<Calculator_, Output_> finisher() {
        return ObjectCalculator::result;
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
