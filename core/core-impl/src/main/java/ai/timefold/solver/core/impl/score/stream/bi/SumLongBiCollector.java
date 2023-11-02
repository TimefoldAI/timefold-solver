package ai.timefold.solver.core.impl.score.stream.bi;

import java.util.function.Supplier;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.impl.score.stream.LongSumCalculator;

public final class SumLongBiCollector<A, B> extends LongCalculatorBiCollector<A, B, Long, LongSumCalculator> {
    SumLongBiCollector(ToLongBiFunction<? super A, ? super B> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongSumCalculator> supplier() {
        return LongSumCalculator::new;
    }
}
