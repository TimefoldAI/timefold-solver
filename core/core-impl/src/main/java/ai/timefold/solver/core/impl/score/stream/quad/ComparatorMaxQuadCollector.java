package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.MinMaxUndoableActionable;

public final class ComparatorMaxQuadCollector<A, B, C, D, Result>
        extends UndoableActionableQuadCollector<A, B, C, D, Result, Result, MinMaxUndoableActionable<Result, Result>> {
    private final Comparator<? super Result> comparator;

    public ComparatorMaxQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result> mapper,
            Comparator<? super Result> comparator) {
        super(mapper);
        this.comparator = comparator;
    }

    @Override
    public Supplier<MinMaxUndoableActionable<Result, Result>> supplier() {
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
        ComparatorMaxQuadCollector<?, ?, ?, ?, ?> that = (ComparatorMaxQuadCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), comparator);
    }
}