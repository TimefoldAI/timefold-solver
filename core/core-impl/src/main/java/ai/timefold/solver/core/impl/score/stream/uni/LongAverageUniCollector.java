package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.impl.score.stream.LongAverageCalculator;

public final class LongAverageUniCollector<A> extends LongCalculatorUniCollector<A, Double, LongAverageCalculator> {
    public LongAverageUniCollector(ToLongFunction<? super A> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongAverageCalculator> supplier() {
        return LongAverageCalculator::new;
    }
}
