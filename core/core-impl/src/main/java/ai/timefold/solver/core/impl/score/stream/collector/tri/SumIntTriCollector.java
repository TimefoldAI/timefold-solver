package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToIntTriFunction;
import ai.timefold.solver.core.impl.score.stream.collector.IntSumCalculator;

final class SumIntTriCollector<A, B, C> extends IntCalculatorTriCollector<A, B, C, Integer, IntSumCalculator> {
    SumIntTriCollector(ToIntTriFunction<? super A, ? super B, ? super C> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntSumCalculator> supplier() {
        return IntSumCalculator::new;
    }
}
