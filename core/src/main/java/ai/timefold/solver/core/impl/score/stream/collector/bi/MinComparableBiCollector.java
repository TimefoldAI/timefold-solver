package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.MinMaxUndoableActionable;

final class MinComparableBiCollector<A, B, Result_ extends Comparable<? super Result_>>
        extends UndoableActionableBiCollector<A, B, Result_, Result_, MinMaxUndoableActionable<Result_, Result_>> {
    MinComparableBiCollector(BiFunction<? super A, ? super B, ? extends Result_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<MinMaxUndoableActionable<Result_, Result_>> supplier() {
        return MinMaxUndoableActionable::minCalculator;
    }
}