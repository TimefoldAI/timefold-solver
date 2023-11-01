package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.Set;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.SetUndoableActionable;

public final class SetTriCollector<A, B, C, Result>
        extends UndoableActionableTriCollector<A, B, C, Result, Set<Result>, SetUndoableActionable<Result>> {
    public SetTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<SetUndoableActionable<Result>> supplier() {
        return SetUndoableActionable::new;
    }
}
