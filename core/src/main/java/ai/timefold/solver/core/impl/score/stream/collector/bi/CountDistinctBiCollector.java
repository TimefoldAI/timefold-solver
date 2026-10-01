package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLongDistinctSlot;

import org.jspecify.annotations.NonNull;

final class CountDistinctBiCollector<A, B, Mapped_>
        extends
        AbstractReferenceBasedBiCollector<A, B, Mapped_, Long, AbstractLongDistinctSlot.State<Mapped_>> {
    CountDistinctBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
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
    protected BiConstraintCollectorValueHandle<A, B> newAccumulatedValue(
            AbstractLongDistinctSlot.State<Mapped_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractLongDistinctSlot<Mapped_>
            implements BiConstraintCollectorValueHandle<A, B> {
        Slot(AbstractLongDistinctSlot.State<Mapped_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b) {
            addMapped(mapper.apply(a, b));
        }

        @Override
        public void replaceWith(A a, B b) {
            replaceWithMapped(mapper.apply(a, b));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
