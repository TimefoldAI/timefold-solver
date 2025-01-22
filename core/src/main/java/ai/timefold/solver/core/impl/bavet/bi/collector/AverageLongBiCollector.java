package ai.timefold.solver.core.impl.bavet.bi.collector;

import java.util.function.Supplier;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.impl.bavet.common.collector.LongAverageCalculator;

import org.jspecify.annotations.NonNull;

final class AverageLongBiCollector<A, B> extends LongCalculatorBiCollector<A, B, Double, LongAverageCalculator> {
    AverageLongBiCollector(ToLongBiFunction<? super A, ? super B> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<LongAverageCalculator> supplier() {
        return LongAverageCalculator::new;
    }
}
