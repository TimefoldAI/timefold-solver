package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.Supplier;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.impl.score.stream.collector.LongSumCalculator;

import org.jspecify.annotations.NonNull;

final class SumBiCollector<A, B> extends LongCalculatorBiCollector<A, B, Long, LongSumCalculator> {
    SumBiCollector(ToLongBiFunction<? super A, ? super B> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<LongSumCalculator> supplier() {
        return LongSumCalculator::new;
    }
}
