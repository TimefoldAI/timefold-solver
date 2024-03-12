package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.collector.MinMaxUndoableActionable;

final class MinComparableTriCollector<A, B, C, Result_ extends Comparable<? super Result_>>
        extends UndoableActionableTriCollector<A, B, C, Result_, Result_, MinMaxUndoableActionable<Result_, Result_>> {
    MinComparableTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<MinMaxUndoableActionable<Result_, Result_>> supplier() {
        return MinMaxUndoableActionable::minCalculator;
    }
}