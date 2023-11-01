package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.MinMaxUndoableActionable;

public final class ComparableMinQuadCollector<A, B, C, D, Result extends Comparable<? super Result>>
        extends UndoableActionableQuadCollector<A, B, C, D, Result, Result, MinMaxUndoableActionable<Result, Result>> {
    public ComparableMinQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<MinMaxUndoableActionable<Result, Result>> supplier() {
        return MinMaxUndoableActionable::minCalculator;
    }
}