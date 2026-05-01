package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.MinMaxUndoableActionable;

import org.jspecify.annotations.NonNull;

final class MaxComparableUniCollector<A, Result_ extends Comparable<? super Result_>>
        extends
        UndoableActionableUniCollector<A, Result_, Result_, MinMaxUndoableActionable.State<Result_, Result_>, MinMaxUndoableActionable<Result_, Result_>> {
    MaxComparableUniCollector(Function<? super A, ? extends Result_> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<MinMaxUndoableActionable.State<Result_, Result_>> supplier() {
        return MinMaxUndoableActionable::maxState;
    }

    @Override
    public @NonNull Function<MinMaxUndoableActionable.State<Result_, Result_>, Result_> finisher() {
        return MinMaxUndoableActionable.State::result;
    }

    @Override
    protected MinMaxUndoableActionable<Result_, Result_> newUndoableActionable(
            MinMaxUndoableActionable.State<Result_, Result_> state) {
        return new MinMaxUndoableActionable<>(state);
    }
}
