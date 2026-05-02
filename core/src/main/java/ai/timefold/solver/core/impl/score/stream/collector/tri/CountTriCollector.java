package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulator;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractCountSlot;

import org.jspecify.annotations.NonNull;

final class CountTriCollector<A, B, C> implements TriConstraintCollector<A, B, C, AbstractCountSlot.State, Long> {
    private static final CountTriCollector<?, ?, ?> INSTANCE = new CountTriCollector<>();

    private CountTriCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A, B, C> CountTriCollector<A, B, C> getInstance() {
        return (CountTriCollector<A, B, C>) INSTANCE;
    }

    @Override
    public @NonNull Supplier<AbstractCountSlot.State> supplier() {
        return AbstractCountSlot.State::new;
    }

    @Override
    public @NonNull QuadFunction<AbstractCountSlot.State, A, B, C, Runnable> accumulator() {
        return TriCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull TriConstraintCollectorAccumulator<AbstractCountSlot.State, A, B, C> incrementalAccumulator() {
        return Slot::new;
    }

    @Override
    public @NonNull Function<AbstractCountSlot.State, Long> finisher() {
        return AbstractCountSlot.State::result;
    }

    private static final class Slot<A, B, C> extends AbstractCountSlot
            implements TriConstraintCollectorAccumulatedValue<A, B, C> {

        Slot(AbstractCountSlot.State state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c) {
            addMapped();
        }

        @Override
        public void update(A a, B b, C c) {
            updateMapped();
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
