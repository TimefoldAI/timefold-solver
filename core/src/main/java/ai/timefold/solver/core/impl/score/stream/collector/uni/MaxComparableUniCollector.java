package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.MinMaxUndoableActionable;

final class MaxComparableUniCollector<A, Result_ extends Comparable<? super Result_>>
        extends UndoableActionableUniCollector<A, Result_, Result_, MinMaxUndoableActionable<Result_, Result_>> {
    MaxComparableUniCollector(Function<? super A, ? extends Result_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<MinMaxUndoableActionable<Result_, Result_>> supplier() {
        return MinMaxUndoableActionable::maxCalculator;
    }
}