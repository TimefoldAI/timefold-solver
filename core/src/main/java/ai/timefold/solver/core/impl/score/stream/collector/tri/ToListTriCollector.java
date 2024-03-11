package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.List;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.collector.ListUndoableActionable;

final class ToListTriCollector<A, B, C, Mapped_>
        extends UndoableActionableTriCollector<A, B, C, Mapped_, List<Mapped_>, ListUndoableActionable<Mapped_>> {
    ToListTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<ListUndoableActionable<Mapped_>> supplier() {
        return ListUndoableActionable::new;
    }
}
