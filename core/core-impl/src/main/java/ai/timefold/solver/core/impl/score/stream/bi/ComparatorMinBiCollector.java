package ai.timefold.solver.core.impl.score.stream.bi;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.MinMaxUndoableActionable;

public final class ComparatorMinBiCollector<A, B, Result>
        extends UndoableActionableBiCollector<A, B, Result, Result, MinMaxUndoableActionable<Result, Result>> {
    private final Comparator<? super Result> comparator;

    public ComparatorMinBiCollector(BiFunction<? super A, ? super B, ? extends Result> mapper,
            Comparator<? super Result> comparator) {
        super(mapper);
        this.comparator = comparator;
    }

    @Override
    public Supplier<MinMaxUndoableActionable<Result, Result>> supplier() {
        return () -> MinMaxUndoableActionable.minCalculator(comparator);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        if (!super.equals(object))
            return false;
        ComparatorMinBiCollector<?, ?, ?> that = (ComparatorMinBiCollector<?, ?, ?>) object;
        return Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), comparator);
    }
}