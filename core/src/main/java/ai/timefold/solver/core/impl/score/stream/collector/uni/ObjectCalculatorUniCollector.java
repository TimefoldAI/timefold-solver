package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;
import ai.timefold.solver.core.impl.score.stream.collector.CollectorUtils;
import ai.timefold.solver.core.impl.score.stream.collector.ObjectCalculator;

import org.jspecify.annotations.NonNull;

abstract sealed class ObjectCalculatorUniCollector<A, Input_, Output_, State_, Calculator_ extends ObjectCalculator<Input_>>
        implements UniConstraintCollector<A, State_, Output_>
        permits AverageReferenceUniCollector, ConnectedRangesUniConstraintCollector, ConsecutiveSequencesUniConstraintCollector,
        CountDistinctUniCollector, SumReferenceUniCollector {

    protected final Function<? super A, ? extends Input_> mapper;

    ObjectCalculatorUniCollector(Function<? super A, ? extends Input_> mapper) {
        this.mapper = Objects.requireNonNull(mapper);
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
        var that = (ObjectCalculatorUniCollector<?, ?, ?, ?, ?>) object;
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
            calculator.insert(mapper.apply(a));
        }

        @Override
        public void update(A a) {
            calculator.update(mapper.apply(a));
        }

        @Override
        public void remove() {
            calculator.retract();
        }
    }
}
