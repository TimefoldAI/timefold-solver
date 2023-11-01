package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToIntQuadFunction;
import ai.timefold.solver.core.impl.score.stream.IntSumCalculator;

public final class IntSumQuadCollector<A, B, C, D> extends IntCalculatorQuadCollector<A, B, C, D, Integer, IntSumCalculator> {
    public IntSumQuadCollector(ToIntQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntSumCalculator> supplier() {
        return IntSumCalculator::new;
    }
}
