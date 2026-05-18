package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorValueHandle;

import org.jspecify.annotations.NonNull;

abstract class ObjectCalculatorTriCollector<A, B, C, Input_, Output_, State_>
        implements TriConstraintCollector<A, B, C, State_, Output_> {

    protected final TriFunction<? super A, ? super B, ? super C, ? extends Input_> mapper;

    public ObjectCalculatorTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Input_> mapper) {
        this.mapper = mapper;
    }

    protected abstract TriConstraintCollectorValueHandle<A, B, C> newAccumulatedValue(State_ state);

    @Override
    public @NonNull TriConstraintCollectorAccumulator<State_, A, B, C> accumulator() {
        return this::newAccumulatedValue;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (ObjectCalculatorTriCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
