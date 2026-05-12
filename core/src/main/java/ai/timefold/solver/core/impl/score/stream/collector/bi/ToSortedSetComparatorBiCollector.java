package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractSortedSetSlot;

import org.jspecify.annotations.NonNull;

final class ToSortedSetComparatorBiCollector<A, B, Mapped_>
        extends
        UndoableActionableBiCollector<A, B, Mapped_, SortedSet<Mapped_>, AbstractSortedSetSlot.State<Mapped_>> {
    private final Comparator<? super Mapped_> comparator;

    ToSortedSetComparatorBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper,
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
    protected BiConstraintCollectorValueHandle<A, B> newAccumulatedValue(
            AbstractSortedSetSlot.State<Mapped_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractSortedSetSlot<Mapped_>
            implements BiConstraintCollectorValueHandle<A, B> {
        Slot(AbstractSortedSetSlot.State<Mapped_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b) {
            addMapped(mapper.apply(a, b));
        }

        @Override
        public void replaceWith(A a, B b) {
            replaceWithMapped(mapper.apply(a, b));
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
        ToSortedSetComparatorBiCollector<?, ?, ?> that = (ToSortedSetComparatorBiCollector<?, ?, ?>) object;
        return Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), comparator);
    }
}
