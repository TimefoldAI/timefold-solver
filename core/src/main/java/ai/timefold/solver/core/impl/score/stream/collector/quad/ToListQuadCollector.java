package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractToListSlot;

import org.jspecify.annotations.NonNull;

final class ToListQuadCollector<A, B, C, D, Mapped_>
        extends
        UndoableActionableQuadCollector<A, B, C, D, Mapped_, List<Mapped_>, AbstractToListSlot.State<Mapped_>> {
    ToListQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper) {
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
    protected QuadConstraintCollectorAccumulatedValue<A, B, C, D> newAccumulatedValue(
            AbstractToListSlot.State<Mapped_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractToListSlot<Mapped_>
            implements QuadConstraintCollectorAccumulatedValue<A, B, C, D> {
        Slot(AbstractToListSlot.State<Mapped_> state) {
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
}
