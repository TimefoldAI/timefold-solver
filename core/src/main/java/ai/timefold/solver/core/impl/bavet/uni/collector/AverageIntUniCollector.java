package ai.timefold.solver.core.impl.bavet.uni.collector;

import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.impl.bavet.common.collector.IntAverageCalculator;

import org.jspecify.annotations.NonNull;

final class AverageIntUniCollector<A> extends IntCalculatorUniCollector<A, Double, IntAverageCalculator> {
    AverageIntUniCollector(ToIntFunction<? super A> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<IntAverageCalculator> supplier() {
        return IntAverageCalculator::new;
    }
}
