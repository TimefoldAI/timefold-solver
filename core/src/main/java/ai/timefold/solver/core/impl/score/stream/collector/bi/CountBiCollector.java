package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractCountSlot;
import ai.timefold.solver.core.impl.util.MutableLong;

import org.jspecify.annotations.NonNull;

final class CountBiCollector<A, B> implements BiConstraintCollector<A, B, MutableLong, Long> {
    private static final CountBiCollector<?, ?> INSTANCE = new CountBiCollector<>();

    private CountBiCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A, B> CountBiCollector<A, B> getInstance() {
        return (CountBiCollector<A, B>) INSTANCE;
    }

    @Override
    public @NonNull Supplier<MutableLong> supplier() {
        return MutableLong::new;
    }

    @Override
    public @NonNull TriFunction<MutableLong, A, B, Runnable> accumulator() {
        return BiCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull BiConstraintCollectorAccumulator<MutableLong, A, B> incrementalAccumulator() {
        return Slot::new;
    }

    @Override
    public @NonNull Function<MutableLong, Long> finisher() {
        return MutableLong::longValue;
    }

    private static final class Slot<A, B> extends AbstractCountSlot
            implements BiConstraintCollectorValueHandle<A, B> {

        Slot(MutableLong state) {
            super(state);
        }

        @Override
        public void add(A a, B b) {
            addMapped();
        }

        @Override
        public void replaceWith(A a, B b) {
            replaceWithMapped();
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
