package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractToListSlot;

import org.jspecify.annotations.NonNull;

final class ToListTriCollector<A, B, C, Mapped_>
        extends
        UndoableActionableTriCollector<A, B, C, Mapped_, List<Mapped_>, AbstractToListSlot.State<Mapped_>> {
    ToListTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<AbstractToListSlot.State<Mapped_>> supplier() {
        return AbstractToListSlot.State::new;
    }

    @Override
    public @NonNull Function<AbstractToListSlot.State<Mapped_>, List<Mapped_>> finisher() {
        return AbstractToListSlot.State::result;
    }

    @Override
    protected TriConstraintCollectorAccumulatedValue<A, B, C> newAccumulatedValue(
            AbstractToListSlot.State<Mapped_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractToListSlot<Mapped_>
            implements TriConstraintCollectorAccumulatedValue<A, B, C> {
        Slot(AbstractToListSlot.State<Mapped_> state) {
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
}
