package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulator;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractCountSlot;

import org.jspecify.annotations.NonNull;

final class CountBiCollector<A, B> implements BiConstraintCollector<A, B, AbstractCountSlot.State, Long> {
    private static final CountBiCollector<?, ?> INSTANCE = new CountBiCollector<>();

    private CountBiCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A, B> CountBiCollector<A, B> getInstance() {
        return (CountBiCollector<A, B>) INSTANCE;
    }

    @Override
    public @NonNull Supplier<AbstractCountSlot.State> supplier() {
        return AbstractCountSlot.State::new;
    }

    @Override
    public @NonNull TriFunction<AbstractCountSlot.State, A, B, Runnable> accumulator() {
        return BiCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull BiConstraintCollectorAccumulator<AbstractCountSlot.State, A, B> incrementalAccumulator() {
        return Slot::new;
    }

    @Override
    public @NonNull Function<AbstractCountSlot.State, Long> finisher() {
        return AbstractCountSlot.State::result;
    }

    private static final class Slot<A, B> extends AbstractCountSlot
            implements BiConstraintCollectorAccumulatedValue<A, B> {

        Slot(AbstractCountSlot.State state) {
            super(state);
        }

        @Override
        public void add(A a, B b) {
            addMapped();
        }

        @Override
        public void update(A a, B b) {
            updateMapped();
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
