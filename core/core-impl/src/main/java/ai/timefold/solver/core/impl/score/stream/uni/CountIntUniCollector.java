package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.IntCounter;

final class CountIntUniCollector<A> implements UniConstraintCollector<A, IntCounter, Integer> {
    CountIntUniCollector() {
    }

    @Override
    public Supplier<IntCounter> supplier() {
        return IntCounter::new;
    }

    @Override
    public BiFunction<IntCounter, A, Runnable> accumulator() {
        return (counter, a) -> {
            counter.increment();
            return counter::decrement;
        };
    }

    @Override
    public Function<IntCounter, Integer> finisher() {
        return IntCounter::result;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CountIntUniCollector;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
