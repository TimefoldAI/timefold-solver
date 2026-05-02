package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLongSumSlot;

import org.jspecify.annotations.NonNull;

final class SumUniCollector<A> extends LongCalculatorUniCollector<A, Long, AbstractLongSumSlot.State> {
    SumUniCollector(ToLongFunction<? super A> mapper) {
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
    protected UniConstraintCollectorAccumulatedValue<A> newAccumulatedValue(AbstractLongSumSlot.State state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractLongSumSlot
            implements UniConstraintCollectorAccumulatedValue<A> {
        Slot(AbstractLongSumSlot.State state) {
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
