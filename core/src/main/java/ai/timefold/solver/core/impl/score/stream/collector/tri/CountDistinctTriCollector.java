package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLongDistinctSlot;

import org.jspecify.annotations.NonNull;

final class CountDistinctTriCollector<A, B, C, Mapped_>
        extends
        ObjectCalculatorTriCollector<A, B, C, Mapped_, Long, AbstractLongDistinctSlot.State<Mapped_>> {
    CountDistinctTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper) {
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
    protected TriConstraintCollectorValueHandle<A, B, C> newAccumulatedValue(
            AbstractLongDistinctSlot.State<Mapped_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractLongDistinctSlot<Mapped_>
            implements TriConstraintCollectorValueHandle<A, B, C> {
        Slot(AbstractLongDistinctSlot.State<Mapped_> state) {
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
