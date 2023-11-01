package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.LongCalculator;

abstract class LongCalculatorUniCollector<A, Output_, Calculator_ extends LongCalculator<Output_>>
        implements UniConstraintCollector<A, Calculator_, Output_> {
    private final ToLongFunction<? super A> mapper;

    public LongCalculatorUniCollector(ToLongFunction<? super A> mapper) {
        this.mapper = mapper;
    }

    @Override
    public BiFunction<Calculator_, A, Runnable> accumulator() {
        return (calculator, a) -> {
            final long mapped = mapper.applyAsLong(a);
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
        LongCalculatorUniCollector<?, ?, ?> that = (LongCalculatorUniCollector<?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
