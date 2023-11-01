package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.MinMaxUndoableActionable;

public final class ComparableMinUniCollector<A, Result extends Comparable<? super Result>>
        extends UndoableActionableUniCollector<A, Result, Result, MinMaxUndoableActionable<Result, Result>> {
    public ComparableMinUniCollector(Function<? super A, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<MinMaxUndoableActionable<Result, Result>> supplier() {
        return MinMaxUndoableActionable::minCalculator;
    }
}