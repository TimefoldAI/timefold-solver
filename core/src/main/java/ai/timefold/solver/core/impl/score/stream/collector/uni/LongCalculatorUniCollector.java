package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;
import ai.timefold.solver.core.impl.score.stream.collector.CollectorUtils;
import ai.timefold.solver.core.impl.score.stream.collector.LongCalculator;

import org.jspecify.annotations.NonNull;

abstract sealed class LongCalculatorUniCollector<A, Output_, State_, Calculator_ extends LongCalculator>
        implements UniConstraintCollector<A, State_, Output_> permits AverageUniCollector, SumUniCollector {
    protected final ToLongFunction<? super A> mapper;

    LongCalculatorUniCollector(ToLongFunction<? super A> mapper) {
        this.mapper = mapper;
    }

    protected abstract Calculator_ newCalculator(State_ state);

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull BiFunction<State_, A, Runnable> accumulator() {
        return CollectorUtils.fromIncrementalUni(incrementalAccumulator());
    }

    @Override
    public @NonNull UniConstraintCollectorAccumulator<State_, A> incrementalAccumulator() {
        return AccumulatedValue::new;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (LongCalculatorUniCollector<?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }

    private final class AccumulatedValue implements UniConstraintCollectorAccumulatedValue<A> {
        private final Calculator_ calculator;

        AccumulatedValue(State_ state) {
            this.calculator = newCalculator(state);
        }

        @Override
        public void add(A a) {
            calculator.insert(mapper.applyAsLong(a));
        }

        @Override
        public void update(A a) {
            calculator.update(mapper.applyAsLong(a));
        }

        @Override
        public void remove() {
            calculator.retract();
        }
    }
}
