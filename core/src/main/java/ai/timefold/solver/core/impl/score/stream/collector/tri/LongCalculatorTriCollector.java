package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulator;

import org.jspecify.annotations.NonNull;

abstract class LongCalculatorTriCollector<A, B, C, Output_, State_>
        implements TriConstraintCollector<A, B, C, State_, Output_> {
    protected final ToLongTriFunction<? super A, ? super B, ? super C> mapper;

    public LongCalculatorTriCollector(ToLongTriFunction<? super A, ? super B, ? super C> mapper) {
        this.mapper = mapper;
    }

    protected abstract TriConstraintCollectorAccumulatedValue<A, B, C> newAccumulatedValue(State_ state);

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull TriConstraintCollectorAccumulator<State_, A, B, C> incrementalAccumulator() {
        return this::newAccumulatedValue;
    }

    @Override
    public @NonNull QuadFunction<State_, A, B, C, Runnable> accumulator() {
        return TriCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (LongCalculatorTriCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
