package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractSortedSetSlot;

import org.jspecify.annotations.NonNull;

final class ToSortedSetComparatorTriCollector<A, B, C, Mapped_>
        extends
        UndoableActionableTriCollector<A, B, C, Mapped_, SortedSet<Mapped_>, AbstractSortedSetSlot.State<Mapped_>> {
    private final Comparator<? super Mapped_> comparator;

    ToSortedSetComparatorTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper,
            Comparator<? super Mapped_> comparator) {
        super(mapper);
        this.comparator = comparator;
    }

    @Override
    public @NonNull Supplier<AbstractSortedSetSlot.State<Mapped_>> supplier() {
        return () -> new AbstractSortedSetSlot.State<>(comparator);
    }

    @Override
    public @NonNull Function<AbstractSortedSetSlot.State<Mapped_>, SortedSet<Mapped_>> finisher() {
        return AbstractSortedSetSlot.State::result;
    }

    @Override
    protected TriConstraintCollectorValueHandle<A, B, C> newAccumulatedValue(
            AbstractSortedSetSlot.State<Mapped_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractSortedSetSlot<Mapped_>
            implements TriConstraintCollectorValueHandle<A, B, C> {
        Slot(AbstractSortedSetSlot.State<Mapped_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c) {
            addMapped(mapper.apply(a, b, c));
        }

        @Override
        public void update(A a, B b, C c) {
            updateMapped(mapper.apply(a, b, c));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        if (!super.equals(object))
            return false;
        ToSortedSetComparatorTriCollector<?, ?, ?, ?> that = (ToSortedSetComparatorTriCollector<?, ?, ?, ?>) object;
        return Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), comparator);
    }
}
