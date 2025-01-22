package ai.timefold.solver.core.impl.bavet.bi.collector;

import java.util.function.Supplier;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.impl.bavet.common.collector.LongSumCalculator;

import org.jspecify.annotations.NonNull;

final class SumLongBiCollector<A, B> extends LongCalculatorBiCollector<A, B, Long, LongSumCalculator> {
    SumLongBiCollector(ToLongBiFunction<? super A, ? super B> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<LongSumCalculator> supplier() {
        return LongSumCalculator::new;
    }
}
