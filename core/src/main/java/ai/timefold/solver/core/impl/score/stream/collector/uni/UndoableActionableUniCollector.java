package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;

import org.jspecify.annotations.NonNull;

abstract class UndoableActionableUniCollector<A, Input_, Output_, State_>
        implements UniConstraintCollector<A, State_, Output_> {
    protected final java.util.function.Function<? super A, ? extends Input_> mapper;

    public UndoableActionableUniCollector(java.util.function.Function<? super A, ? extends Input_> mapper) {
        this.mapper = mapper;
    }

    protected abstract UniConstraintCollectorAccumulatedValue<A> newAccumulatedValue(State_ state);

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
        var that = (UndoableActionableUniCollector<?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
