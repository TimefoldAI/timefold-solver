package ai.timefold.solver.core.impl.score.stream.bi;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.SetUndoableActionable;

public final class SetBiCollector<A, B, Result>
        extends UndoableActionableBiCollector<A, B, Result, Set<Result>, SetUndoableActionable<Result>> {
    public SetBiCollector(BiFunction<? super A, ? super B, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<SetUndoableActionable<Result>> supplier() {
        return SetUndoableActionable::new;
    }
}
