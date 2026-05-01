package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.ListUndoableActionable;

import org.jspecify.annotations.NonNull;

final class ToListBiCollector<A, B, Mapped_>
        extends
        UndoableActionableBiCollector<A, B, Mapped_, List<Mapped_>, ListUndoableActionable.State<Mapped_>, ListUndoableActionable<Mapped_>> {
    ToListBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<ListUndoableActionable.State<Mapped_>> supplier() {
        return ListUndoableActionable.State::new;
    }

    @Override
    public @NonNull Function<ListUndoableActionable.State<Mapped_>, List<Mapped_>> finisher() {
        return ListUndoableActionable.State::result;
    }

    @Override
    protected ListUndoableActionable<Mapped_> newUndoableActionable(ListUndoableActionable.State<Mapped_> state) {
        return new ListUndoableActionable<>(state);
    }
}
