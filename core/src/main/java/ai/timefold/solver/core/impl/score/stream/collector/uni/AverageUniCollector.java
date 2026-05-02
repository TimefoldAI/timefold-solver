package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLongAverageSlot;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class AverageUniCollector<A>
        extends LongCalculatorUniCollector<A, Double, AbstractLongAverageSlot.State> {
    AverageUniCollector(ToLongFunction<? super A> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<AbstractLongAverageSlot.State> supplier() {
        return AbstractLongAverageSlot.State::new;
    }

    @Override
    public @NonNull Function<AbstractLongAverageSlot.State, @Nullable Double> finisher() {
        return AbstractLongAverageSlot.State::result;
    }

    @Override
    protected UniConstraintCollectorAccumulatedValue<A> newAccumulatedValue(AbstractLongAverageSlot.State state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractLongAverageSlot
            implements UniConstraintCollectorAccumulatedValue<A> {
        Slot(AbstractLongAverageSlot.State state) {
            super(state);
        }

        @Override
        public void add(A a) {
            addMapped(mapper.applyAsLong(a));
        }

        @Override
        public void update(A a) {
            updateMapped(mapper.applyAsLong(a));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
