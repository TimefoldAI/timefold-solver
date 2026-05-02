package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLongSumSlot;

import org.jspecify.annotations.NonNull;

final class SumBiCollector<A, B>
        extends LongCalculatorBiCollector<A, B, Long, AbstractLongSumSlot.State> {
    SumBiCollector(ToLongBiFunction<? super A, ? super B> mapper) {
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
    protected BiConstraintCollectorAccumulatedValue<A, B> newAccumulatedValue(AbstractLongSumSlot.State state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractLongSumSlot
            implements BiConstraintCollectorAccumulatedValue<A, B> {
        Slot(AbstractLongSumSlot.State state) {
            super(state);
        }

        @Override
        public void add(A a, B b) {
            addMapped(mapper.applyAsLong(a, b));
        }

        @Override
        public void update(A a, B b) {
            updateMapped(mapper.applyAsLong(a, b));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
