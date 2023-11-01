package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.IntDistinctCountCalculator;

public final class IntDistinctCountQuadCollector<A, B, C, D, Result>
        extends ObjectCalculatorQuadCollector<A, B, C, D, Result, Integer, IntDistinctCountCalculator<Result>> {
    public IntDistinctCountQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntDistinctCountCalculator<Result>> supplier() {
        return IntDistinctCountCalculator::new;
    }
}
