package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.ListUndoableActionable;

final class ToListBiCollector<A, B, Mapped_>
        extends UndoableActionableBiCollector<A, B, Mapped_, List<Mapped_>, ListUndoableActionable<Mapped_>> {
    ToListBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<ListUndoableActionable<Mapped_>> supplier() {
        return ListUndoableActionable::new;
    }
}
