package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.SortedSetUndoableActionable;

public final class ComparatorSortedSetQuadCollector<A, B, C, D, Mapped_>
        extends UndoableActionableQuadCollector<A, B, C, D, Mapped_, SortedSet<Mapped_>, SortedSetUndoableActionable<Mapped_>> {
    private final Comparator<? super Mapped_> comparator;

    public ComparatorSortedSetQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper,
            Comparator<? super Mapped_> comparator) {
        super(mapper);
        this.comparator = comparator;
    }

    @Override
    public Supplier<SortedSetUndoableActionable<Mapped_>> supplier() {
        return () -> SortedSetUndoableActionable.orderBy(comparator);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        if (!super.equals(object))
            return false;
        ComparatorSortedSetQuadCollector<?, ?, ?, ?, ?> that = (ComparatorSortedSetQuadCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), comparator);
    }
}
