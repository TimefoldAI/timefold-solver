package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.UndoableActionable;

import org.jspecify.annotations.NonNull;

abstract sealed class UndoableActionableBiCollector<A, B, Input_, Output_, State_, Calculator_ extends UndoableActionable<Input_>>
        implements BiConstraintCollector<A, B, State_, Output_>
        permits MaxComparableBiCollector, MaxComparatorBiCollector, MaxPropertyBiCollector, MinComparableBiCollector,
        MinComparatorBiCollector, MinPropertyBiCollector, ToCollectionBiCollector, ToListBiCollector, ToMultiMapBiCollector,
        ToSetBiCollector, ToSimpleMapBiCollector, ToSortedSetComparatorBiCollector {
    private final BiFunction<? super A, ? super B, ? extends Input_> mapper;

    public UndoableActionableBiCollector(BiFunction<? super A, ? super B, ? extends Input_> mapper) {
        this.mapper = mapper;
    }

    protected abstract Calculator_ newUndoableActionable(State_ state);

    @Override
    public @NonNull TriFunction<State_, A, B, Runnable> accumulator() {
        return (state, a, b) -> {
            var ua = newUndoableActionable(state);
            ua.insert(mapper.apply(a, b));
            return ua::retract;
        };
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (UndoableActionableBiCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
