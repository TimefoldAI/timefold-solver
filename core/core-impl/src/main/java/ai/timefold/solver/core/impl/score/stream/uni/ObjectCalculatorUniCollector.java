package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.ObjectCalculator;

abstract sealed class ObjectCalculatorUniCollector<A, Input_, Output_, Calculator_ extends ObjectCalculator<Input_, Output_>>
        implements UniConstraintCollector<A, Calculator_, Output_>
        permits AverageReferenceUniCollector, CountDistinctIntUniCollector, CountDistinctLongUniCollector,
        SumReferenceUniCollector {
    private final Function<? super A, ? extends Input_> mapper;

    public ObjectCalculatorUniCollector(Function<? super A, ? extends Input_> mapper) {
        this.mapper = mapper;
    }

    @Override
    public BiFunction<Calculator_, A, Runnable> accumulator() {
        return (calculator, a) -> {
            final Input_ mapped = mapper.apply(a);
            calculator.insert(mapped);
            return () -> calculator.retract(mapped);
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
        var that = (ObjectCalculatorUniCollector<?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
