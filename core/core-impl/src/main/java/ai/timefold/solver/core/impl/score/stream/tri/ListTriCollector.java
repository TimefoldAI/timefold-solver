package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.List;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.ListUndoableActionable;

public final class ListTriCollector<A, B, C, Result>
        extends UndoableActionableTriCollector<A, B, C, Result, List<Result>, ListUndoableActionable<Result>> {
    public ListTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<ListUndoableActionable<Result>> supplier() {
        return ListUndoableActionable::new;
    }
}
