package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.UndoableActionable;

abstract sealed class UndoableActionableBiCollector<A, B, Input_, Output_, Calculator_ extends UndoableActionable<Input_, Output_>>
        implements BiConstraintCollector<A, B, Calculator_, Output_>
        permits MaxComparableBiCollector, MaxComparatorBiCollector, MaxPropertyBiCollector, MinComparableBiCollector,
        MinComparatorBiCollector, MinPropertyBiCollector, ToCollectionBiCollector, ToListBiCollector, ToMultiMapBiCollector,
        ToSetBiCollector, ToSimpleMapBiCollector, ToSortedSetComparatorBiCollector {
    private final BiFunction<? super A, ? super B, ? extends Input_> mapper;

    public UndoableActionableBiCollector(BiFunction<? super A, ? super B, ? extends Input_> mapper) {
        this.mapper = mapper;
    }

    @Override
    public TriFunction<Calculator_, A, B, Runnable> accumulator() {
        return (calculator, a, b) -> {
            final Input_ mapped = mapper.apply(a, b);
            return calculator.insert(mapped);
        };
    }

    @Override
    public Function<Calculator_, Output_> finisher() {
        return UndoableActionable::result;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (UndoableActionableBiCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
