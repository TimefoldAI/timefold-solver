package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.MinMaxUndoableActionable;

public final class ComparableMinTriCollector<A, B, C, Result extends Comparable<? super Result>>
        extends UndoableActionableTriCollector<A, B, C, Result, Result, MinMaxUndoableActionable<Result, Result>> {
    public ComparableMinTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<MinMaxUndoableActionable<Result, Result>> supplier() {
        return MinMaxUndoableActionable::minCalculator;
    }
}