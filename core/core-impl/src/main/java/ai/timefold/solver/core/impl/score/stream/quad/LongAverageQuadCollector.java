package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.impl.score.stream.LongAverageCalculator;

public final class LongAverageQuadCollector<A, B, C, D>
        extends LongCalculatorQuadCollector<A, B, C, D, Double, LongAverageCalculator> {
    public LongAverageQuadCollector(ToLongQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongAverageCalculator> supplier() {
        return LongAverageCalculator::new;
    }
}
