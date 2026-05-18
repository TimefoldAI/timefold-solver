package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;

import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorValueHandle;

import org.jspecify.annotations.NonNull;

abstract class LongCalculatorQuadCollector<A, B, C, D, Output_, State_>
        implements QuadConstraintCollector<A, B, C, D, State_, Output_> {
    protected final ToLongQuadFunction<? super A, ? super B, ? super C, ? super D> mapper;

    public LongCalculatorQuadCollector(ToLongQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
        this.mapper = mapper;
    }

    protected abstract QuadConstraintCollectorValueHandle<A, B, C, D> newAccumulatedValue(State_ state);

    @Override
    public @NonNull QuadConstraintCollectorAccumulator<State_, A, B, C, D> accumulator() {
        return this::newAccumulatedValue;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (LongCalculatorQuadCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
