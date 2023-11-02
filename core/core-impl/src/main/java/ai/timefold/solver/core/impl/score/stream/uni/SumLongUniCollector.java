package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.impl.score.stream.LongSumCalculator;

final class SumLongUniCollector<A> extends LongCalculatorUniCollector<A, Long, LongSumCalculator> {
    SumLongUniCollector(ToLongFunction<? super A> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongSumCalculator> supplier() {
        return LongSumCalculator::new;
    }
}
