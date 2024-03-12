package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.MinMaxUndoableActionable;

final class MaxComparableQuadCollector<A, B, C, D, Result_ extends Comparable<? super Result_>>
        extends UndoableActionableQuadCollector<A, B, C, D, Result_, Result_, MinMaxUndoableActionable<Result_, Result_>> {
    MaxComparableQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<MinMaxUndoableActionable<Result_, Result_>> supplier() {
        return MinMaxUndoableActionable::maxCalculator;
    }
}