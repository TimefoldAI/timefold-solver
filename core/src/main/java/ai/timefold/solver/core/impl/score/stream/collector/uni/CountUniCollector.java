package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractCountSlot;
import ai.timefold.solver.core.impl.util.MutableLong;

import org.jspecify.annotations.NonNull;

final class CountUniCollector<A> implements UniConstraintCollector<A, MutableLong, Long> {
    private static final CountUniCollector<?> INSTANCE = new CountUniCollector<>();

    private CountUniCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A> CountUniCollector<A> getInstance() {
        return (CountUniCollector<A>) INSTANCE;
    }

    @Override
    public @NonNull Supplier<MutableLong> supplier() {
        return MutableLong::new;
    }

    @Override
    public @NonNull BiFunction<MutableLong, A, Runnable> accumulator() {
        return UniCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull UniConstraintCollectorAccumulator<MutableLong, A> incrementalAccumulator() {
        return Slot::new;
    }

    @Override
    public @NonNull Function<MutableLong, Long> finisher() {
        return MutableLong::longValue;
    }

    private static final class Slot<A> extends AbstractCountSlot
            implements UniConstraintCollectorValueHandle<A> {

        Slot(MutableLong state) {
            super(state);
        }

        @Override
        public void add(A a) {
            addMapped();
        }

        @Override
        public void replaceWith(A a) {
            replaceWithMapped();
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
