package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.ObjectCalculator;

abstract class ObjectCalculatorTriCollector<A, B, C, Input_, Output_, Calculator_ extends ObjectCalculator<Input_, Output_>>
        implements TriConstraintCollector<A, B, C, Calculator_, Output_> {
    private final TriFunction<? super A, ? super B, ? super C, ? extends Input_> mapper;

    public ObjectCalculatorTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Input_> mapper) {
        this.mapper = mapper;
    }

    @Override
    public QuadFunction<Calculator_, A, B, C, Runnable> accumulator() {
        return (calculator, a, b, c) -> {
            final Input_ mapped = mapper.apply(a, b, c);
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
        ObjectCalculatorTriCollector<?, ?, ?, ?, ?, ?> that = (ObjectCalculatorTriCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
