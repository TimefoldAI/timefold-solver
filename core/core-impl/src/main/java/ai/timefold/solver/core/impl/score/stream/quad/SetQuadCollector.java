package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.Set;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.SetUndoableActionable;

public final class SetQuadCollector<A, B, C, D, Result>
        extends UndoableActionableQuadCollector<A, B, C, D, Result, Set<Result>, SetUndoableActionable<Result>> {
    public SetQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<SetUndoableActionable<Result>> supplier() {
        return SetUndoableActionable::new;
    }
}
