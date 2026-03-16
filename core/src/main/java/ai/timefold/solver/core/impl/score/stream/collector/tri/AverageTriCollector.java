package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.impl.score.stream.collector.LongAverageCalculator;

import org.jspecify.annotations.NonNull;

final class AverageTriCollector<A, B, C> extends LongCalculatorTriCollector<A, B, C, Double, LongAverageCalculator> {
    AverageTriCollector(ToLongTriFunction<? super A, ? super B, ? super C> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<LongAverageCalculator> supplier() {
        return LongAverageCalculator::new;
    }
}
