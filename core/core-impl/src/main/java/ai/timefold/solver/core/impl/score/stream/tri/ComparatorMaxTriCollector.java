package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.MinMaxUndoableActionable;

public final class ComparatorMaxTriCollector<A, B, C, Result>
        extends UndoableActionableTriCollector<A, B, C, Result, Result, MinMaxUndoableActionable<Result, Result>> {
    private final Comparator<? super Result> comparator;

    public ComparatorMaxTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Result> mapper,
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
        ComparatorMaxTriCollector<?, ?, ?, ?> that = (ComparatorMaxTriCollector<?, ?, ?, ?>) object;
        return Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), comparator);
    }
}