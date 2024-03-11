package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.UndoableActionable;

abstract sealed class UndoableActionableQuadCollector<A, B, C, D, Input_, Output_, Calculator_ extends UndoableActionable<Input_, Output_>>
        implements QuadConstraintCollector<A, B, C, D, Calculator_, Output_>
        permits MaxComparableQuadCollector, MaxComparatorQuadCollector, MaxPropertyQuadCollector, MinComparableQuadCollector,
        MinComparatorQuadCollector, MinPropertyQuadCollector, ToCollectionQuadCollector, ToListQuadCollector,
        ToMultiMapQuadCollector, ToSetQuadCollector, ToSimpleMapQuadCollector, ToSortedSetComparatorQuadCollector {
    private final QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Input_> mapper;

    public UndoableActionableQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Input_> mapper) {
        this.mapper = mapper;
    }

    @Override
    public PentaFunction<Calculator_, A, B, C, D, Runnable> accumulator() {
        return (calculator, a, b, c, d) -> {
            final Input_ mapped = mapper.apply(a, b, c, d);
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
        var that = (UndoableActionableQuadCollector<?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }
}
