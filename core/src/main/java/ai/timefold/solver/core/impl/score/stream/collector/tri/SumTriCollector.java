package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLongSumSlot;
import ai.timefold.solver.core.impl.util.MutableLong;

import org.jspecify.annotations.NonNull;

final class SumTriCollector<A, B, C>
        extends LongCalculatorTriCollector<A, B, C, Long, MutableLong> {
    SumTriCollector(ToLongTriFunction<? super A, ? super B, ? super C> mapper) {
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
    protected TriConstraintCollectorValueHandle<A, B, C> newAccumulatedValue(MutableLong state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractLongSumSlot
            implements TriConstraintCollectorValueHandle<A, B, C> {
        Slot(MutableLong state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c) {
            addMapped(mapper.applyAsLong(a, b, c));
        }

        @Override
        public void replaceWith(A a, B b, C c) {
            replaceWithMapped(mapper.applyAsLong(a, b, c));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
