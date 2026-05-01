package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;
import ai.timefold.solver.core.impl.score.stream.collector.CollectorUtils;
import ai.timefold.solver.core.impl.score.stream.collector.LongCounter;

import org.jspecify.annotations.NonNull;

final class CountUniCollector<A> implements UniConstraintCollector<A, LongCounter, Long> {
    private static final CountUniCollector<?> INSTANCE = new CountUniCollector<>();

    private CountUniCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A> CountUniCollector<A> getInstance() {
        return (CountUniCollector<A>) INSTANCE;
    }

    @Override
    public @NonNull Supplier<LongCounter> supplier() {
        return LongCounter::new;
    }

    @Override
    public @NonNull BiFunction<LongCounter, A, Runnable> accumulator() {
        return CollectorUtils.fromIncrementalUni(incrementalAccumulator());
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public @NonNull UniConstraintCollectorAccumulator<LongCounter, A> incrementalAccumulator() {
        return AccumulatedValue::new;
    }

    @Override
    public @NonNull Function<LongCounter, Long> finisher() {
        return LongCounter::result;
    }

    private record AccumulatedValue<A>(LongCounter container)
            implements
                UniConstraintCollectorAccumulatedValue<A> {

        @Override
        public void add(A a) {
            container.increment();
        }

        @Override
        public void update(A a) {
            // count unchanged on update
        }

        @Override
        public void remove() {
            container.decrement();
        }
    }
}
