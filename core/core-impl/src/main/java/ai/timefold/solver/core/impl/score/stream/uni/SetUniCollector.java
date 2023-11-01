package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.SetUndoableActionable;

public final class SetUniCollector<A, Result>
        extends UndoableActionableUniCollector<A, Result, Set<Result>, SetUndoableActionable<Result>> {
    public SetUniCollector(Function<? super A, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<SetUndoableActionable<Result>> supplier() {
        return SetUndoableActionable::new;
    }
}
