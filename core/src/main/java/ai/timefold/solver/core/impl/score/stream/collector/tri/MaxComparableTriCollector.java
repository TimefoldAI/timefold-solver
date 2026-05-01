package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.collector.MinMaxUndoableActionable;

import org.jspecify.annotations.NonNull;

final class MaxComparableTriCollector<A, B, C, Result_ extends Comparable<? super Result_>>
        extends
        UndoableActionableTriCollector<A, B, C, Result_, Result_, MinMaxUndoableActionable.State<Result_, Result_>, MinMaxUndoableActionable<Result_, Result_>> {
    MaxComparableTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<MinMaxUndoableActionable.State<Result_, Result_>> supplier() {
        return MinMaxUndoableActionable::maxState;
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
