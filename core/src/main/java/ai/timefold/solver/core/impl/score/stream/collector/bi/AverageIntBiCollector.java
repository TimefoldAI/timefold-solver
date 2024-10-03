package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.Supplier;
import java.util.function.ToIntBiFunction;

import ai.timefold.solver.core.impl.score.stream.collector.IntAverageCalculator;

import org.jspecify.annotations.NonNull;

final class AverageIntBiCollector<A, B> extends IntCalculatorBiCollector<A, B, Double, IntAverageCalculator> {
    AverageIntBiCollector(ToIntBiFunction<? super A, ? super B> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<IntAverageCalculator> supplier() {
        return IntAverageCalculator::new;
    }
}
