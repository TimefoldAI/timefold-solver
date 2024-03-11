package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.UndoableActionable;

abstract sealed class UndoableActionableTriCollector<A, B, C, Input_, Output_, Calculator_ extends UndoableActionable<Input_, Output_>>
        implements TriConstraintCollector<A, B, C, Calculator_, Output_>
        permits MaxComparableTriCollector, MaxComparatorTriCollector, MaxPropertyTriCollector, MinComparableTriCollector,
        MinComparatorTriCollector, MinPropertyTriCollector, ToCollectionTriCollector, ToListTriCollector,
        ToMultiMapTriCollector, ToSetTriCollector, ToSimpleMapTriCollector, ToSortedSetComparatorTriCollector {
    private final TriFunction<? super A, ? super B, ? super C, ? extends Input_> mapper;

    public UndoableActionableTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Input_> mapper) {
        this.mapper = mapper;
    }

    @Override
    public QuadFunction<Calculator_, A, B, C, Runnable> accumulator() {
        return (calculator, a, b, c) -> {
            final Input_ mapped = mapper.apply(a, b, c);
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
        var that = (UndoableActionableTriCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
