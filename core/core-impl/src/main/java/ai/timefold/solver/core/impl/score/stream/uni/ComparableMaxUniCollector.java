package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.MinMaxUndoableActionable;

public final class ComparableMaxUniCollector<A, Result_ extends Comparable<? super Result_>>
        extends UndoableActionableUniCollector<A, Result_, Result_, MinMaxUndoableActionable<Result_, Result_>> {
    public ComparableMaxUniCollector(Function<? super A, ? extends Result_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<MinMaxUndoableActionable<Result_, Result_>> supplier() {
        return MinMaxUndoableActionable::maxCalculator;
    }
}