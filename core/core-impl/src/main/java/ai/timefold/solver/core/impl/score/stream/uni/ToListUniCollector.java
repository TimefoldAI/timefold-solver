package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.ListUndoableActionable;

public final class ToListUniCollector<A, Mapped_>
        extends UndoableActionableUniCollector<A, Mapped_, List<Mapped_>, ListUndoableActionable<Mapped_>> {
    ToListUniCollector(Function<? super A, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<ListUndoableActionable<Mapped_>> supplier() {
        return ListUndoableActionable::new;
    }
}
