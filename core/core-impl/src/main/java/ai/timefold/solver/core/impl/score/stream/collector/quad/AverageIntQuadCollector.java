package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToIntQuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.IntAverageCalculator;

final class AverageIntQuadCollector<A, B, C, D>
        extends IntCalculatorQuadCollector<A, B, C, D, Double, IntAverageCalculator> {
    AverageIntQuadCollector(ToIntQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntAverageCalculator> supplier() {
        return IntAverageCalculator::new;
    }
}
