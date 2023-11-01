package ai.timefold.solver.core.impl.score.stream.bi;

import java.util.function.Supplier;
import java.util.function.ToIntBiFunction;

import ai.timefold.solver.core.impl.score.stream.IntSumCalculator;

public final class IntSumBiCollector<A, B> extends IntCalculatorBiCollector<A, B, Integer, IntSumCalculator> {
    public IntSumBiCollector(ToIntBiFunction<? super A, ? super B> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntSumCalculator> supplier() {
        return IntSumCalculator::new;
    }
}
