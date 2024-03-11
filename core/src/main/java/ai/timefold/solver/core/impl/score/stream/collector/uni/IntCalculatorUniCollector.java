package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.IntCalculator;

abstract sealed class IntCalculatorUniCollector<A, Output_, Calculator_ extends IntCalculator<Output_>>
        implements UniConstraintCollector<A, Calculator_, Output_> permits AverageIntUniCollector, SumIntUniCollector {
    private final ToIntFunction<? super A> mapper;

    public IntCalculatorUniCollector(ToIntFunction<? super A> mapper) {
        this.mapper = mapper;
    }

    @Override
    public BiFunction<Calculator_, A, Runnable> accumulator() {
        return (calculator, a) -> {
            final int mapped = mapper.applyAsInt(a);
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
        var that = (IntCalculatorUniCollector<?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
