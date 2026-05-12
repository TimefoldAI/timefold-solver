package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;

import org.jspecify.annotations.NonNull;

abstract class LongCalculatorUniCollector<A, Output_, State_>
        implements UniConstraintCollector<A, State_, Output_> {
    protected final ToLongFunction<? super A> mapper;

    LongCalculatorUniCollector(ToLongFunction<? super A> mapper) {
        this.mapper = mapper;
    }

    protected abstract UniConstraintCollectorValueHandle<A> newAccumulatedValue(State_ state);

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull BiFunction<State_, A, Runnable> accumulator() {
        return UniCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public @NonNull UniConstraintCollectorAccumulator<State_, A> incrementalAccumulator() {
        return this::newAccumulatedValue;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (LongCalculatorUniCollector<?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }

}
