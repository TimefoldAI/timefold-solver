package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.LongDistinctCountCalculator;

public final class LongDistinctCountTriCollector<A, B, C, Result>
        extends ObjectCalculatorTriCollector<A, B, C, Result, Long, LongDistinctCountCalculator<Result>> {
    public LongDistinctCountTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongDistinctCountCalculator<Result>> supplier() {
        return LongDistinctCountCalculator::new;
    }
}
