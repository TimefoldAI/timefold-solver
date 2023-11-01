package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.LongDistinctCountCalculator;

public final class LongDistinctCountUniCollector<A, Result>
        extends ObjectCalculatorUniCollector<A, Result, Long, LongDistinctCountCalculator<Result>> {
    public LongDistinctCountUniCollector(Function<? super A, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongDistinctCountCalculator<Result>> supplier() {
        return LongDistinctCountCalculator::new;
    }
}
