package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractToListSlot;

import org.jspecify.annotations.NonNull;

final class ToListBiCollector<A, B, Mapped_>
        extends
        UndoableActionableBiCollector<A, B, Mapped_, List<Mapped_>, AbstractToListSlot.State<Mapped_>> {
    ToListBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
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
    protected BiConstraintCollectorAccumulatedValue<A, B> newAccumulatedValue(AbstractToListSlot.State<Mapped_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractToListSlot<Mapped_>
            implements BiConstraintCollectorAccumulatedValue<A, B> {
        Slot(AbstractToListSlot.State<Mapped_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b) {
            addMapped(mapper.apply(a, b));
        }

        @Override
        public void update(A a, B b) {
            updateMapped(mapper.apply(a, b));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
