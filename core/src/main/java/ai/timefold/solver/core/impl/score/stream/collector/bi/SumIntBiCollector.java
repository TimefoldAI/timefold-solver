package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.Supplier;
import java.util.function.ToIntBiFunction;

import ai.timefold.solver.core.impl.score.stream.collector.IntSumCalculator;

final class SumIntBiCollector<A, B> extends IntCalculatorBiCollector<A, B, Integer, IntSumCalculator> {
    SumIntBiCollector(ToIntBiFunction<? super A, ? super B> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntSumCalculator> supplier() {
        return IntSumCalculator::new;
    }
}
