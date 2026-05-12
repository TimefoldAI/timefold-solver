package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLongDistinctSlot;

import org.jspecify.annotations.NonNull;

final class CountDistinctUniCollector<A, Mapped_>
        extends ObjectCalculatorUniCollector<A, Mapped_, Long, AbstractLongDistinctSlot.State<Mapped_>> {
    CountDistinctUniCollector(Function<? super A, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<AbstractLongDistinctSlot.State<Mapped_>> supplier() {
        return AbstractLongDistinctSlot.State::new;
    }

    @Override
    public @NonNull Function<AbstractLongDistinctSlot.State<Mapped_>, Long> finisher() {
        return AbstractLongDistinctSlot.State::result;
    }

    @Override
    protected UniConstraintCollectorValueHandle<A>
            newAccumulatedValue(AbstractLongDistinctSlot.State<Mapped_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractLongDistinctSlot<Mapped_>
            implements UniConstraintCollectorValueHandle<A> {
        Slot(AbstractLongDistinctSlot.State<Mapped_> state) {
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
}
