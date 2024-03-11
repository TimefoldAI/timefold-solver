package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.SortedSetUndoableActionable;

final class ToSortedSetComparatorBiCollector<A, B, Mapped_>
        extends UndoableActionableBiCollector<A, B, Mapped_, SortedSet<Mapped_>, SortedSetUndoableActionable<Mapped_>> {
    private final Comparator<? super Mapped_> comparator;

    ToSortedSetComparatorBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper,
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
        ToSortedSetComparatorBiCollector<?, ?, ?> that = (ToSortedSetComparatorBiCollector<?, ?, ?>) object;
        return Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), comparator);
    }
}
