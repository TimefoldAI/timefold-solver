package ai.timefold.solver.core.impl.bavet.uni.collector;

import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.impl.bavet.common.collector.IntSumCalculator;

import org.jspecify.annotations.NonNull;

final class SumIntUniCollector<A> extends IntCalculatorUniCollector<A, Integer, IntSumCalculator> {
    SumIntUniCollector(ToIntFunction<? super A> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<IntSumCalculator> supplier() {
        return IntSumCalculator::new;
    }
}
