package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.UndoableActionable;

abstract sealed class UndoableActionableUniCollector<A, Input_, Output_, Calculator_ extends UndoableActionable<Input_, Output_>>
        implements UniConstraintCollector<A, Calculator_, Output_>
        permits MaxComparableUniCollector, MaxComparatorUniCollector, MaxPropertyUniCollector, MinComparableUniCollector,
        MinComparatorUniCollector, MinPropertyUniCollector, ToCollectionUniCollector, ToListUniCollector,
        ToMultiMapUniCollector, ToSetUniCollector, ToSimpleMapUniCollector, ToSortedSetComparatorUniCollector {
    private final Function<? super A, ? extends Input_> mapper;

    public UndoableActionableUniCollector(Function<? super A, ? extends Input_> mapper) {
        this.mapper = mapper;
    }

    @Override
    public BiFunction<Calculator_, A, Runnable> accumulator() {
        return (calculator, a) -> {
            final Input_ mapped = mapper.apply(a);
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
        var that = (UndoableActionableUniCollector<?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
