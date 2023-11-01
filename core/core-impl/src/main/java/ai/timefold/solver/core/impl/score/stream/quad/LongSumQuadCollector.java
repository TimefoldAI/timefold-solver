package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.impl.score.stream.LongSumCalculator;

public final class LongSumQuadCollector<A, B, C, D> extends LongCalculatorQuadCollector<A, B, C, D, Long, LongSumCalculator> {
    public LongSumQuadCollector(ToLongQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongSumCalculator> supplier() {
        return LongSumCalculator::new;
    }
}
