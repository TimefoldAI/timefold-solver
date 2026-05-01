package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.collector.MinMaxUndoableActionable;

import org.jspecify.annotations.NonNull;

final class MaxComparatorTriCollector<A, B, C, Result_>
        extends
        UndoableActionableTriCollector<A, B, C, Result_, Result_, MinMaxUndoableActionable.State<Result_, Result_>, MinMaxUndoableActionable<Result_, Result_>> {
    private final Comparator<? super Result_> comparator;

    MaxComparatorTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper,
            Comparator<? super Result_> comparator) {
        super(mapper);
        this.comparator = comparator;
    }

    @Override
    public @NonNull Supplier<MinMaxUndoableActionable.State<Result_, Result_>> supplier() {
        return () -> MinMaxUndoableActionable.maxState(comparator);
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

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        if (!super.equals(object))
            return false;
        MaxComparatorTriCollector<?, ?, ?, ?> that = (MaxComparatorTriCollector<?, ?, ?, ?>) object;
        return Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), comparator);
    }
}
