package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.LongCounter;

public final class CountLongUniCollector<A> implements UniConstraintCollector<A, LongCounter, Long> {
    CountLongUniCollector() {
    }

    @Override
    public Supplier<LongCounter> supplier() {
        return LongCounter::new;
    }

    @Override
    public BiFunction<LongCounter, A, Runnable> accumulator() {
        return (counter, a) -> {
            counter.increment();
            return counter::decrement;
        };
    }

    @Override
    public Function<LongCounter, Long> finisher() {
        return LongCounter::result;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CountLongUniCollector;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
