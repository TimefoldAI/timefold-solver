package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLongAverageSlot;

import org.jspecify.annotations.NonNull;

final class AverageBiCollector<A, B>
        extends LongCalculatorBiCollector<A, B, Double, AbstractLongAverageSlot.State> {
    AverageBiCollector(ToLongBiFunction<? super A, ? super B> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<AbstractLongAverageSlot.State> supplier() {
        return AbstractLongAverageSlot.State::new;
    }

    @Override
    public @NonNull Function<AbstractLongAverageSlot.State, Double> finisher() {
        return AbstractLongAverageSlot.State::result;
    }

    @Override
    protected BiConstraintCollectorAccumulatedValue<A, B> newAccumulatedValue(AbstractLongAverageSlot.State state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractLongAverageSlot
            implements BiConstraintCollectorAccumulatedValue<A, B> {
        Slot(AbstractLongAverageSlot.State state) {
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
