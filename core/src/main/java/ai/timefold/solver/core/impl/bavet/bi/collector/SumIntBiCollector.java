package ai.timefold.solver.core.impl.bavet.bi.collector;

import java.util.function.Supplier;
import java.util.function.ToIntBiFunction;

import ai.timefold.solver.core.impl.bavet.common.collector.IntSumCalculator;

import org.jspecify.annotations.NonNull;

final class SumIntBiCollector<A, B> extends IntCalculatorBiCollector<A, B, Integer, IntSumCalculator> {
    SumIntBiCollector(ToIntBiFunction<? super A, ? super B> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<IntSumCalculator> supplier() {
        return IntSumCalculator::new;
    }
}
