package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.ListUndoableActionable;

public final class ListUniCollector<A, Mapped_>
        extends UndoableActionableUniCollector<A, Mapped_, List<Mapped_>, ListUndoableActionable<Mapped_>> {
    public ListUniCollector(Function<? super A, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<ListUndoableActionable<Mapped_>> supplier() {
        return ListUndoableActionable::new;
    }
}
