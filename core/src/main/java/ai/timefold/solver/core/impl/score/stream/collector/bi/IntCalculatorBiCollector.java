package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.IntCalculator;

abstract sealed class IntCalculatorBiCollector<A, B, Output_, Calculator_ extends IntCalculator<Output_>>
        implements BiConstraintCollector<A, B, Calculator_, Output_> permits AverageIntBiCollector, SumIntBiCollector {
    private final ToIntBiFunction<? super A, ? super B> mapper;

    public IntCalculatorBiCollector(ToIntBiFunction<? super A, ? super B> mapper) {
        this.mapper = mapper;
    }

    @Override
    public TriFunction<Calculator_, A, B, Runnable> accumulator() {
        return (calculator, a, b) -> {
            final int mapped = mapper.applyAsInt(a, b);
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
        var that = (IntCalculatorBiCollector<?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
