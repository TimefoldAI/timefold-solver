package ai.timefold.solver.core.impl.score.stream.bi;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.LongDistinctCountCalculator;

public final class LongDistinctCountBiCollector<A, B, Result>
        extends ObjectCalculatorBiCollector<A, B, Result, Long, LongDistinctCountCalculator<Result>> {
    public LongDistinctCountBiCollector(BiFunction<? super A, ? super B, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongDistinctCountCalculator<Result>> supplier() {
        return LongDistinctCountCalculator::new;
    }
}
