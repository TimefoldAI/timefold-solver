package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToIntQuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.IntSumCalculator;

final class SumIntQuadCollector<A, B, C, D> extends IntCalculatorQuadCollector<A, B, C, D, Integer, IntSumCalculator> {
    SumIntQuadCollector(ToIntQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntSumCalculator> supplier() {
        return IntSumCalculator::new;
    }
}
