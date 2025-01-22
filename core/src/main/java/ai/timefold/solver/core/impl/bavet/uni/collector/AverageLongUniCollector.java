package ai.timefold.solver.core.impl.bavet.uni.collector;

import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.impl.bavet.common.collector.LongAverageCalculator;

import org.jspecify.annotations.NonNull;

final class AverageLongUniCollector<A> extends LongCalculatorUniCollector<A, Double, LongAverageCalculator> {
    AverageLongUniCollector(ToLongFunction<? super A> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<LongAverageCalculator> supplier() {
        return LongAverageCalculator::new;
    }
}
