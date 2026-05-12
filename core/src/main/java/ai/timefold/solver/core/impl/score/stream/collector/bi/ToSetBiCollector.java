package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractToSetSlot;

import org.jspecify.annotations.NonNull;

final class ToSetBiCollector<A, B, Mapped_>
        extends
        UndoableActionableBiCollector<A, B, Mapped_, Set<Mapped_>, AbstractToSetSlot.State<Mapped_>> {
    ToSetBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<AbstractToSetSlot.State<Mapped_>> supplier() {
        return AbstractToSetSlot.State::new;
    }

    @Override
    public @NonNull Function<AbstractToSetSlot.State<Mapped_>, Set<Mapped_>> finisher() {
        return AbstractToSetSlot.State::result;
    }

    @Override
    protected BiConstraintCollectorValueHandle<A, B> newAccumulatedValue(AbstractToSetSlot.State<Mapped_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractToSetSlot<Mapped_>
            implements BiConstraintCollectorValueHandle<A, B> {
        Slot(AbstractToSetSlot.State<Mapped_> state) {
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
