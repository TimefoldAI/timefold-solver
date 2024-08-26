package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.ObjectCalculator;

abstract sealed class ObjectCalculatorUniCollector<A, Input_, Output_, Mapped_, Calculator_ extends ObjectCalculator<Input_, Output_, Mapped_>>
        implements UniConstraintCollector<A, Calculator_, Output_>
        permits AverageReferenceUniCollector, ConnectedRangesUniConstraintCollector, ConsecutiveSequencesUniConstraintCollector,
        CountDistinctIntUniCollector, CountDistinctLongUniCollector, SumReferenceUniCollector {

    protected final Function<? super A, ? extends Input_> mapper;

    public ObjectCalculatorUniCollector(Function<? super A, ? extends Input_> mapper) {
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public BiFunction<Calculator_, A, Runnable> accumulator() {
        return (calculator, a) -> {
            final var mapped = mapper.apply(a);
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
        var that = (ObjectCalculatorUniCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
