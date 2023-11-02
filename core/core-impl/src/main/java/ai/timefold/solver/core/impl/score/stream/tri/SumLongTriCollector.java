package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.impl.score.stream.LongSumCalculator;

final class SumLongTriCollector<A, B, C> extends LongCalculatorTriCollector<A, B, C, Long, LongSumCalculator> {
    SumLongTriCollector(ToLongTriFunction<? super A, ? super B, ? super C> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongSumCalculator> supplier() {
        return LongSumCalculator::new;
    }
}
