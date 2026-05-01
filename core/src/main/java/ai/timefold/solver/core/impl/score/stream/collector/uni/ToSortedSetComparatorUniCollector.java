package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.SortedSetUndoableActionable;

import org.jspecify.annotations.NonNull;

final class ToSortedSetComparatorUniCollector<A, Mapped_>
        extends
        UndoableActionableUniCollector<A, Mapped_, SortedSet<Mapped_>, SortedSetUndoableActionable.State<Mapped_>, SortedSetUndoableActionable<Mapped_>> {
    private final Comparator<? super Mapped_> comparator;

    ToSortedSetComparatorUniCollector(Function<? super A, ? extends Mapped_> mapper,
            Comparator<? super Mapped_> comparator) {
        super(mapper);
        this.comparator = comparator;
    }

    @Override
    public @NonNull Supplier<SortedSetUndoableActionable.State<Mapped_>> supplier() {
        return () -> new SortedSetUndoableActionable.State<>(comparator);
    }

    @Override
    public @NonNull Function<SortedSetUndoableActionable.State<Mapped_>, SortedSet<Mapped_>> finisher() {
        return SortedSetUndoableActionable.State::result;
    }

    @Override
    protected SortedSetUndoableActionable<Mapped_> newUndoableActionable(SortedSetUndoableActionable.State<Mapped_> state) {
        return new SortedSetUndoableActionable<>(state);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        if (!super.equals(object))
            return false;
        ToSortedSetComparatorUniCollector<?, ?> that = (ToSortedSetComparatorUniCollector<?, ?>) object;
        return Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), comparator);
    }
}
