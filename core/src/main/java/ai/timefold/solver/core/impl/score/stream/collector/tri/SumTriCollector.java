package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLongSumSlot;

import org.jspecify.annotations.NonNull;

final class SumTriCollector<A, B, C>
        extends LongCalculatorTriCollector<A, B, C, Long, AbstractLongSumSlot.State> {
    SumTriCollector(ToLongTriFunction<? super A, ? super B, ? super C> mapper) {
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
    protected TriConstraintCollectorAccumulatedValue<A, B, C> newAccumulatedValue(AbstractLongSumSlot.State state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractLongSumSlot
            implements TriConstraintCollectorAccumulatedValue<A, B, C> {
        Slot(AbstractLongSumSlot.State state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c) {
            addMapped(mapper.applyAsLong(a, b, c));
        }

        @Override
        public void update(A a, B b, C c) {
            updateMapped(mapper.applyAsLong(a, b, c));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
