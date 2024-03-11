package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.Supplier;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.impl.score.stream.collector.LongSumCalculator;

final class SumLongBiCollector<A, B> extends LongCalculatorBiCollector<A, B, Long, LongSumCalculator> {
    SumLongBiCollector(ToLongBiFunction<? super A, ? super B> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongSumCalculator> supplier() {
        return LongSumCalculator::new;
    }
}
