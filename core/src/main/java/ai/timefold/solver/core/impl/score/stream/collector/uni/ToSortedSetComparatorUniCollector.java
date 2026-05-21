package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractSortedSetSlot;

import org.jspecify.annotations.NonNull;

final class ToSortedSetComparatorUniCollector<A, Mapped_>
        extends AbstractReferenceBasedUniCollector<A, Mapped_, SortedSet<Mapped_>, AbstractSortedSetSlot.State<Mapped_>> {
    private final Comparator<? super Mapped_> comparator;

    ToSortedSetComparatorUniCollector(Function<? super A, ? extends Mapped_> mapper,
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
    protected UniConstraintCollectorValueHandle<A>
            newAccumulatedValue(AbstractSortedSetSlot.State<Mapped_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractSortedSetSlot<Mapped_>
            implements UniConstraintCollectorValueHandle<A> {
        Slot(AbstractSortedSetSlot.State<Mapped_> state) {
            super(state);
        }

        @Override
        public void add(A a) {
            addMapped(mapper.apply(a));
        }

        @Override
        public void replaceWith(A a) {
            replaceWithMapped(mapper.apply(a));
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
        ToSortedSetComparatorUniCollector<?, ?> that = (ToSortedSetComparatorUniCollector<?, ?>) object;
        return Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), comparator);
    }
}
