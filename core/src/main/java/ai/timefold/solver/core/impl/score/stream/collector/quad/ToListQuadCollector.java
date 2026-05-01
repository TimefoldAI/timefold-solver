package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.ListUndoableActionable;

import org.jspecify.annotations.NonNull;

final class ToListQuadCollector<A, B, C, D, Mapped_>
        extends
        UndoableActionableQuadCollector<A, B, C, D, Mapped_, List<Mapped_>, ListUndoableActionable.State<Mapped_>, ListUndoableActionable<Mapped_>> {
    ToListQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper) {
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
