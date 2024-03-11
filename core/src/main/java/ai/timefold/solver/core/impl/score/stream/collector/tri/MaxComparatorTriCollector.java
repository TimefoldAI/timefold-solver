package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.collector.MinMaxUndoableActionable;

final class MaxComparatorTriCollector<A, B, C, Result_>
        extends UndoableActionableTriCollector<A, B, C, Result_, Result_, MinMaxUndoableActionable<Result_, Result_>> {
    private final Comparator<? super Result_> comparator;

    MaxComparatorTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper,
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
        MaxComparatorTriCollector<?, ?, ?, ?> that = (MaxComparatorTriCollector<?, ?, ?, ?>) object;
        return Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), comparator);
    }
}