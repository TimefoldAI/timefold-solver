package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.IntDistinctCountCalculator;

public final class IntDistinctCountUniCollector<A, Result>
        extends ObjectCalculatorUniCollector<A, Result, Integer, IntDistinctCountCalculator<Result>> {
    public IntDistinctCountUniCollector(Function<? super A, ? extends Result> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntDistinctCountCalculator<Result>> supplier() {
        return IntDistinctCountCalculator::new;
    }
}
