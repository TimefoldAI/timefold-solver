package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;

import org.jspecify.annotations.NonNull;

abstract class UndoableActionableUniCollector<A, Input_, Output_, State_>
        implements UniConstraintCollector<A, State_, Output_> {
    protected final java.util.function.Function<? super A, ? extends Input_> mapper;

    public UndoableActionableUniCollector(java.util.function.Function<? super A, ? extends Input_> mapper) {
        this.mapper = mapper;
    }

    protected abstract UniConstraintCollectorValueHandle<A> newAccumulatedValue(State_ state);

    @Override
    public @NonNull UniConstraintCollectorAccumulator<State_, A> accumulator() {
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
