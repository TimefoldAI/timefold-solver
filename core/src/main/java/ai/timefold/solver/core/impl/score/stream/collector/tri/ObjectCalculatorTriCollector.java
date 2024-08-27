package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.ObjectCalculator;

abstract sealed class ObjectCalculatorTriCollector<A, B, C, Input_, Output_, Mapped_, Calculator_ extends ObjectCalculator<Input_, Output_, Mapped_>>
        implements TriConstraintCollector<A, B, C, Calculator_, Output_>
        permits AverageReferenceTriCollector, ConnectedRangesTriConstraintCollector, ConsecutiveSequencesTriConstraintCollector,
        CountDistinctIntTriCollector, CountDistinctLongTriCollector, SumReferenceTriCollector {
    protected final TriFunction<? super A, ? super B, ? super C, ? extends Input_> mapper;

    public ObjectCalculatorTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Input_> mapper) {
        this.mapper = mapper;
    }

    @Override
    public QuadFunction<Calculator_, A, B, C, Runnable> accumulator() {
        return (calculator, a, b, c) -> {
            final var mapped = mapper.apply(a, b, c);
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
        var that = (ObjectCalculatorTriCollector<?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
