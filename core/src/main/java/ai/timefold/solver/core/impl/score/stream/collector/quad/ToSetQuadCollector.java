package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractToSetSlot;

import org.jspecify.annotations.NonNull;

final class ToSetQuadCollector<A, B, C, D, Mapped_>
        extends
        UndoableActionableQuadCollector<A, B, C, D, Mapped_, Set<Mapped_>, AbstractToSetSlot.State<Mapped_>> {
    ToSetQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper) {
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
    protected QuadConstraintCollectorValueHandle<A, B, C, D> newAccumulatedValue(
            AbstractToSetSlot.State<Mapped_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractToSetSlot<Mapped_>
            implements QuadConstraintCollectorValueHandle<A, B, C, D> {
        Slot(AbstractToSetSlot.State<Mapped_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c, D d) {
            addMapped(mapper.apply(a, b, c, d));
        }

        @Override
        public void replaceWith(A a, B b, C c, D d) {
            replaceWithMapped(mapper.apply(a, b, c, d));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
