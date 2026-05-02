package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulator;

import org.jspecify.annotations.NonNull;

abstract class LongCalculatorBiCollector<A, B, Output_, State_>
        implements BiConstraintCollector<A, B, State_, Output_> {
    protected final ToLongBiFunction<? super A, ? super B> mapper;

    LongCalculatorBiCollector(ToLongBiFunction<? super A, ? super B> mapper) {
        this.mapper = mapper;
    }

    protected abstract BiConstraintCollectorAccumulatedValue<A, B> newAccumulatedValue(State_ state);

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull BiConstraintCollectorAccumulator<State_, A, B> incrementalAccumulator() {
        return this::newAccumulatedValue;
    }

    @Override
    public @NonNull TriFunction<State_, A, B, Runnable> accumulator() {
        return BiCollectorUtils.fromIncremental(incrementalAccumulator());
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
