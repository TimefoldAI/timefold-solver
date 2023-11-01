package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.ListUndoableActionable;

public final class ListUniCollector<A, Result>
        extends UndoableActionableUniCollector<A, Result, List<Result>, ListUndoableActionable<Result>> {
    public ListUniCollector(Function<? super A, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<ListUndoableActionable<Result>> supplier() {
        return ListUndoableActionable::new;
    }
}
