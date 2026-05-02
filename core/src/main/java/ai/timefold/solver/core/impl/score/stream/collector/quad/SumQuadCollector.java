package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLongSumSlot;

import org.jspecify.annotations.NonNull;

final class SumQuadCollector<A, B, C, D>
        extends LongCalculatorQuadCollector<A, B, C, D, Long, AbstractLongSumSlot.State> {
    SumQuadCollector(ToLongQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<AbstractLongSumSlot.State> supplier() {
        return AbstractLongSumSlot.State::new;
    }

    @Override
    public @NonNull Function<AbstractLongSumSlot.State, Long> finisher() {
        return AbstractLongSumSlot.State::result;
    }

    @Override
    protected QuadConstraintCollectorAccumulatedValue<A, B, C, D> newAccumulatedValue(AbstractLongSumSlot.State state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractLongSumSlot
            implements QuadConstraintCollectorAccumulatedValue<A, B, C, D> {
        Slot(AbstractLongSumSlot.State state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c, D d) {
            addMapped(mapper.applyAsLong(a, b, c, d));
        }

        @Override
        public void update(A a, B b, C c, D d) {
            updateMapped(mapper.applyAsLong(a, b, c, d));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
