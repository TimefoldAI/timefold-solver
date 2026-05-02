package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractToSetSlot;

import org.jspecify.annotations.NonNull;

final class ToSetUniCollector<A, Mapped_>
        extends UndoableActionableUniCollector<A, Mapped_, Set<Mapped_>, AbstractToSetSlot.State<Mapped_>> {
    ToSetUniCollector(Function<? super A, ? extends Mapped_> mapper) {
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
    protected UniConstraintCollectorAccumulatedValue<A>
            newAccumulatedValue(AbstractToSetSlot.State<Mapped_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractToSetSlot<Mapped_>
            implements UniConstraintCollectorAccumulatedValue<A> {
        Slot(AbstractToSetSlot.State<Mapped_> state) {
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
