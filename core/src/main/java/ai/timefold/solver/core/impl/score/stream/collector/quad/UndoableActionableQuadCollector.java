package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorValueHandle;

import org.jspecify.annotations.NonNull;

abstract class UndoableActionableQuadCollector<A, B, C, D, Input_, Output_, State_>
        implements QuadConstraintCollector<A, B, C, D, State_, Output_> {
    protected final QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Input_> mapper;

    public UndoableActionableQuadCollector(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Input_> mapper) {
        this.mapper = mapper;
    }

    protected abstract QuadConstraintCollectorValueHandle<A, B, C, D> newAccumulatedValue(State_ state);

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull QuadConstraintCollectorAccumulator<State_, A, B, C, D> incrementalAccumulator() {
        return this::newAccumulatedValue;
    }

    @Override
    public @NonNull PentaFunction<State_, A, B, C, D, Runnable> accumulator() {
        return QuadCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (UndoableActionableQuadCollector<?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
