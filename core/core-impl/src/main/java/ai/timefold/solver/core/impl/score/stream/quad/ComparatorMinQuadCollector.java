package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.MinMaxUndoableActionable;

public final class ComparatorMinQuadCollector<A, B, C, D, Result>
        extends UndoableActionableQuadCollector<A, B, C, D, Result, Result, MinMaxUndoableActionable<Result, Result>> {
    private final Comparator<? super Result> comparator;

    public ComparatorMinQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result> mapper,
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
        ComparatorMinQuadCollector<?, ?, ?, ?, ?> that = (ComparatorMinQuadCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), comparator);
    }
}