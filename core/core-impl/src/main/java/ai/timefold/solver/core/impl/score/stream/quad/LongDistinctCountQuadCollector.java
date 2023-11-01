package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.LongDistinctCountCalculator;

public final class LongDistinctCountQuadCollector<A, B, C, D, Result>
        extends ObjectCalculatorQuadCollector<A, B, C, D, Result, Long, LongDistinctCountCalculator<Result>> {
    public LongDistinctCountQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongDistinctCountCalculator<Result>> supplier() {
        return LongDistinctCountCalculator::new;
    }
}
