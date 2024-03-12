package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.List;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.ListUndoableActionable;

final class ToListQuadCollector<A, B, C, D, Mapped_>
        extends UndoableActionableQuadCollector<A, B, C, D, Mapped_, List<Mapped_>, ListUndoableActionable<Mapped_>> {
    ToListQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<ListUndoableActionable<Mapped_>> supplier() {
        return ListUndoableActionable::new;
    }
}
