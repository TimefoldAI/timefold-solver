package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToIntTriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.IntCalculator;

abstract class IntCalculatorTriCollector<A, B, C, Output_, Calculator_ extends IntCalculator<Output_>>
        implements TriConstraintCollector<A, B, C, Calculator_, Output_> {
    private final ToIntTriFunction<? super A, ? super B, ? super C> mapper;

    public IntCalculatorTriCollector(ToIntTriFunction<? super A, ? super B, ? super C> mapper) {
        this.mapper = mapper;
    }

    @Override
    public QuadFunction<Calculator_, A, B, C, Runnable> accumulator() {
        return (calculator, a, b, c) -> {
            final int mapped = mapper.applyAsInt(a, b, c);
            calculator.insert(mapped);
            return () -> calculator.retract(mapped);
        };
    }

    @Override
    public Function<Calculator_, Output_> finisher() {
        return IntCalculator::result;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        IntCalculatorTriCollector<?, ?, ?, ?, ?> that = (IntCalculatorTriCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
