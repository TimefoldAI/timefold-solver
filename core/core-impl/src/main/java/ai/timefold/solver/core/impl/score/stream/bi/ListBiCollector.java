package ai.timefold.solver.core.impl.score.stream.bi;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.ListUndoableActionable;

public final class ListBiCollector<A, B, Result>
        extends UndoableActionableBiCollector<A, B, Result, List<Result>, ListUndoableActionable<Result>> {
    public ListBiCollector(BiFunction<? super A, ? super B, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<ListUndoableActionable<Result>> supplier() {
        return ListUndoableActionable::new;
    }
}
