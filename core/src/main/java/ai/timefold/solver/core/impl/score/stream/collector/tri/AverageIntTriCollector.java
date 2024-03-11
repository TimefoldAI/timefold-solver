package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToIntTriFunction;
import ai.timefold.solver.core.impl.score.stream.collector.IntAverageCalculator;

final class AverageIntTriCollector<A, B, C> extends IntCalculatorTriCollector<A, B, C, Double, IntAverageCalculator> {
    AverageIntTriCollector(ToIntTriFunction<? super A, ? super B, ? super C> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntAverageCalculator> supplier() {
        return IntAverageCalculator::new;
    }
}
