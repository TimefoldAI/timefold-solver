package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractToSetSlot;

import org.jspecify.annotations.NonNull;

final class ToSetTriCollector<A, B, C, Mapped_>
        extends
        UndoableActionableTriCollector<A, B, C, Mapped_, Set<Mapped_>, AbstractToSetSlot.State<Mapped_>> {
    ToSetTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper) {
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
    protected TriConstraintCollectorValueHandle<A, B, C> newAccumulatedValue(
            AbstractToSetSlot.State<Mapped_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractToSetSlot<Mapped_>
            implements TriConstraintCollectorValueHandle<A, B, C> {
        Slot(AbstractToSetSlot.State<Mapped_> state) {
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
