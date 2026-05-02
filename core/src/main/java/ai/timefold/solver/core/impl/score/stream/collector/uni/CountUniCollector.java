package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractCountSlot;

import org.jspecify.annotations.NonNull;

final class CountUniCollector<A> implements UniConstraintCollector<A, AbstractCountSlot.State, Long> {
    private static final CountUniCollector<?> INSTANCE = new CountUniCollector<>();

    private CountUniCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A> CountUniCollector<A> getInstance() {
        return (CountUniCollector<A>) INSTANCE;
    }

    @Override
    public @NonNull Supplier<AbstractCountSlot.State> supplier() {
        return AbstractCountSlot.State::new;
    }

    @Override
    public @NonNull BiFunction<AbstractCountSlot.State, A, Runnable> accumulator() {
        return UniCollectorUtils.fromIncremental(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull UniConstraintCollectorAccumulator<AbstractCountSlot.State, A> incrementalAccumulator() {
        return Slot::new;
    }

    @Override
    public @NonNull Function<AbstractCountSlot.State, Long> finisher() {
        return AbstractCountSlot.State::result;
    }

    private static final class Slot<A> extends AbstractCountSlot
            implements UniConstraintCollectorAccumulatedValue<A> {

        Slot(AbstractCountSlot.State state) {
            super(state);
        }

        @Override
        public void add(A a) {
            addMapped();
        }

        @Override
        public void update(A a) {
            updateMapped();
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
