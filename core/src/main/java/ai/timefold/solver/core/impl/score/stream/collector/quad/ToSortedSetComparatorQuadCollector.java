package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractSortedSetSlot;

import org.jspecify.annotations.NonNull;

final class ToSortedSetComparatorQuadCollector<A, B, C, D, Mapped_>
        extends
        UndoableActionableQuadCollector<A, B, C, D, Mapped_, SortedSet<Mapped_>, AbstractSortedSetSlot.State<Mapped_>> {
    private final Comparator<? super Mapped_> comparator;

    ToSortedSetComparatorQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper,
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
    protected QuadConstraintCollectorAccumulatedValue<A, B, C, D> newAccumulatedValue(
            AbstractSortedSetSlot.State<Mapped_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractSortedSetSlot<Mapped_>
            implements QuadConstraintCollectorAccumulatedValue<A, B, C, D> {
        Slot(AbstractSortedSetSlot.State<Mapped_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c, D d) {
            addMapped(mapper.apply(a, b, c, d));
        }

        @Override
        public void update(A a, B b, C c, D d) {
            updateMapped(mapper.apply(a, b, c, d));
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
        ToSortedSetComparatorQuadCollector<?, ?, ?, ?, ?> that = (ToSortedSetComparatorQuadCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), comparator);
    }
}
