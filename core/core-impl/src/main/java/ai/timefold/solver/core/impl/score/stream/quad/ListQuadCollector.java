package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.List;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.ListUndoableActionable;

public final class ListQuadCollector<A, B, C, D, Result>
        extends UndoableActionableQuadCollector<A, B, C, D, Result, List<Result>, ListUndoableActionable<Result>> {
    public ListQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<ListUndoableActionable<Result>> supplier() {
        return ListUndoableActionable::new;
    }
}
