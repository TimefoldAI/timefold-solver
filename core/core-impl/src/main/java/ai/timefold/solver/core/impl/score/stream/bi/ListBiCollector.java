package ai.timefold.solver.core.impl.score.stream.bi;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.ListUndoableActionable;

public final class ListBiCollector<A, B, Mapped_>
        extends UndoableActionableBiCollector<A, B, Mapped_, List<Mapped_>, ListUndoableActionable<Mapped_>> {
    public ListBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<ListUndoableActionable<Mapped_>> supplier() {
        return ListUndoableActionable::new;
    }
}
