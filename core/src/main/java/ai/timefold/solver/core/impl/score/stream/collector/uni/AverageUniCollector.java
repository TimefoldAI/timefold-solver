package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.impl.score.stream.collector.LongAverageCalculator;

import org.jspecify.annotations.NonNull;

final class AverageUniCollector<A> extends LongCalculatorUniCollector<A, Double, LongAverageCalculator> {
    AverageUniCollector(ToLongFunction<? super A> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<LongAverageCalculator> supplier() {
        return LongAverageCalculator::new;
    }
}
