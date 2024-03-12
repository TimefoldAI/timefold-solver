package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.LongCalculator;

abstract sealed class LongCalculatorBiCollector<A, B, Output_, Calculator_ extends LongCalculator<Output_>>
        implements BiConstraintCollector<A, B, Calculator_, Output_> permits AverageLongBiCollector, SumLongBiCollector {
    private final ToLongBiFunction<? super A, ? super B> mapper;

    public LongCalculatorBiCollector(ToLongBiFunction<? super A, ? super B> mapper) {
        this.mapper = mapper;
    }

    @Override
    public TriFunction<Calculator_, A, B, Runnable> accumulator() {
        return (calculator, a, b) -> {
            final long mapped = mapper.applyAsLong(a, b);
            calculator.insert(mapped);
            return () -> calculator.retract(mapped);
        };
    }

    @Override
    public Function<Calculator_, Output_> finisher() {
        return LongCalculator::result;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (LongCalculatorBiCollector<?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
