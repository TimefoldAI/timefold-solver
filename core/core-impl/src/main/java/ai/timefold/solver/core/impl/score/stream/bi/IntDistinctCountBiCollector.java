package ai.timefold.solver.core.impl.score.stream.bi;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.IntDistinctCountCalculator;

public final class IntDistinctCountBiCollector<A, B, Result>
        extends ObjectCalculatorBiCollector<A, B, Result, Integer, IntDistinctCountCalculator<Result>> {
    public IntDistinctCountBiCollector(BiFunction<? super A, ? super B, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntDistinctCountCalculator<Result>> supplier() {
        return IntDistinctCountCalculator::new;
    }
}
