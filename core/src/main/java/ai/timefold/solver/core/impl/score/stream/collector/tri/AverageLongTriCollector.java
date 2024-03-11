package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.impl.score.stream.collector.LongAverageCalculator;

final class AverageLongTriCollector<A, B, C> extends LongCalculatorTriCollector<A, B, C, Double, LongAverageCalculator> {
    AverageLongTriCollector(ToLongTriFunction<? super A, ? super B, ? super C> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongAverageCalculator> supplier() {
        return LongAverageCalculator::new;
    }
}
