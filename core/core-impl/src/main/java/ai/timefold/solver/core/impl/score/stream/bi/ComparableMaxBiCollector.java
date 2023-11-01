package ai.timefold.solver.core.impl.score.stream.bi;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.MinMaxUndoableActionable;

public final class ComparableMaxBiCollector<A, B, Result extends Comparable<? super Result>>
        extends UndoableActionableBiCollector<A, B, Result, Result, MinMaxUndoableActionable<Result, Result>> {
    public ComparableMaxBiCollector(BiFunction<? super A, ? super B, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<MinMaxUndoableActionable<Result, Result>> supplier() {
        return MinMaxUndoableActionable::maxCalculator;
    }
}