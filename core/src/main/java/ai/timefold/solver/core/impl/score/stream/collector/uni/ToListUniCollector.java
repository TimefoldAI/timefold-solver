package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractToListSlot;

import org.jspecify.annotations.NonNull;

final class ToListUniCollector<A, Mapped_>
        extends UndoableActionableUniCollector<A, Mapped_, List<Mapped_>, AbstractToListSlot.State<Mapped_>> {
    ToListUniCollector(Function<? super A, ? extends Mapped_> mapper) {
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
    protected UniConstraintCollectorValueHandle<A> newAccumulatedValue(AbstractToListSlot.State<Mapped_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractToListSlot<Mapped_>
            implements UniConstraintCollectorValueHandle<A> {
        Slot(AbstractToListSlot.State<Mapped_> state) {
            super(state);
        }

        @Override
        public void add(A a) {
            addMapped(mapper.apply(a));
        }

        @Override
        public void update(A a) {
            updateMapped(mapper.apply(a));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
