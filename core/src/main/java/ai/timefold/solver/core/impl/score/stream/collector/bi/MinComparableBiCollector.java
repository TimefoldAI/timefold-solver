package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.MinMaxUndoableActionable;

import org.jspecify.annotations.NonNull;

final class MinComparableBiCollector<A, B, Result_ extends Comparable<? super Result_>>
        extends
        UndoableActionableBiCollector<A, B, Result_, Result_, MinMaxUndoableActionable.State<Result_, Result_>, MinMaxUndoableActionable<Result_, Result_>> {
    MinComparableBiCollector(BiFunction<? super A, ? super B, ? extends Result_> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<MinMaxUndoableActionable.State<Result_, Result_>> supplier() {
        return MinMaxUndoableActionable::minState;
    }

    @Override
    public @NonNull Function<MinMaxUndoableActionable.State<Result_, Result_>, Result_> finisher() {
        return state -> state.result();
    }

    @Override
    protected MinMaxUndoableActionable<Result_, Result_> newUndoableActionable(
            MinMaxUndoableActionable.State<Result_, Result_> state) {
        return new MinMaxUndoableActionable<>(state);
    }
}
