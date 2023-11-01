package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.IntDistinctCountCalculator;

public final class IntDistinctCountTriCollector<A, B, C, Result>
        extends ObjectCalculatorTriCollector<A, B, C, Result, Integer, IntDistinctCountCalculator<Result>> {
    public IntDistinctCountTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntDistinctCountCalculator<Result>> supplier() {
        return IntDistinctCountCalculator::new;
    }
}
