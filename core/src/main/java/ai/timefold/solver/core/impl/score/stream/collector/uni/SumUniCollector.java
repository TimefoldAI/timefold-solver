package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLongSumSlot;
import ai.timefold.solver.core.impl.util.MutableLong;

import org.jspecify.annotations.NonNull;

final class SumUniCollector<A> extends LongCalculatorUniCollector<A, Long, MutableLong> {
    SumUniCollector(ToLongFunction<? super A> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<MutableLong> supplier() {
        return MutableLong::new;
    }

    @Override
    public @NonNull Function<MutableLong, Long> finisher() {
        return MutableLong::longValue;
    }

    @Override
    protected UniConstraintCollectorValueHandle<A> newAccumulatedValue(MutableLong state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractLongSumSlot
            implements UniConstraintCollectorValueHandle<A> {

        Slot(MutableLong state) {
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
