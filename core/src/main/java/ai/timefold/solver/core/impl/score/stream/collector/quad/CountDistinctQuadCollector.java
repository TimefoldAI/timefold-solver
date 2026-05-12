package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLongDistinctSlot;

import org.jspecify.annotations.NonNull;

final class CountDistinctQuadCollector<A, B, C, D, Mapped_>
        extends
        ObjectCalculatorQuadCollector<A, B, C, D, Mapped_, Long, AbstractLongDistinctSlot.State<Mapped_>> {
    CountDistinctQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper) {
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
    protected QuadConstraintCollectorValueHandle<A, B, C, D> newAccumulatedValue(
            AbstractLongDistinctSlot.State<Mapped_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractLongDistinctSlot<Mapped_>
            implements QuadConstraintCollectorValueHandle<A, B, C, D> {
        Slot(AbstractLongDistinctSlot.State<Mapped_> state) {
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
