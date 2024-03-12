package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.MinMaxUndoableActionable;

final class MaxComparatorBiCollector<A, B, Result_>
        extends UndoableActionableBiCollector<A, B, Result_, Result_, MinMaxUndoableActionable<Result_, Result_>> {
    private final Comparator<? super Result_> comparator;

    MaxComparatorBiCollector(BiFunction<? super A, ? super B, ? extends Result_> mapper,
            Comparator<? super Result_> comparator) {
        super(mapper);
        this.comparator = comparator;
    }

    @Override
    public Supplier<MinMaxUndoableActionable<Result_, Result_>> supplier() {
        return () -> MinMaxUndoableActionable.maxCalculator(comparator);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        if (!super.equals(object))
            return false;
        MaxComparatorBiCollector<?, ?, ?> that = (MaxComparatorBiCollector<?, ?, ?>) object;
        return Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), comparator);
    }
}