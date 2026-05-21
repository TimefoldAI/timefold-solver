package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorValueHandle;

import org.jspecify.annotations.NonNull;

abstract class AbstractPrimitiveBasedBiCollector<A, B, Output_, State_>
        implements BiConstraintCollector<A, B, State_, Output_> {
    protected final ToLongBiFunction<? super A, ? super B> mapper;

    AbstractPrimitiveBasedBiCollector(ToLongBiFunction<? super A, ? super B> mapper) {
        this.mapper = mapper;
    }

    protected abstract BiConstraintCollectorValueHandle<A, B> newAccumulatedValue(State_ state);

    @Override
    public @NonNull BiConstraintCollectorAccumulator<State_, A, B> accumulator() {
        return this::newAccumulatedValue;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (AbstractPrimitiveBasedBiCollector<?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
