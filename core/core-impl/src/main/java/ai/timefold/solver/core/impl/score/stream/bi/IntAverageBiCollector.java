package ai.timefold.solver.core.impl.score.stream.bi;

import java.util.function.Supplier;
import java.util.function.ToIntBiFunction;

import ai.timefold.solver.core.impl.score.stream.IntAverageCalculator;

public final class IntAverageBiCollector<A, B> extends IntCalculatorBiCollector<A, B, Double, IntAverageCalculator> {
    public IntAverageBiCollector(ToIntBiFunction<? super A, ? super B> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntAverageCalculator> supplier() {
        return IntAverageCalculator::new;
    }
}
