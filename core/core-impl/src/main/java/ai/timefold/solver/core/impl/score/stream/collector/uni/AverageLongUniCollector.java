package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.impl.score.stream.collector.LongAverageCalculator;

final class AverageLongUniCollector<A> extends LongCalculatorUniCollector<A, Double, LongAverageCalculator> {
    AverageLongUniCollector(ToLongFunction<? super A> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongAverageCalculator> supplier() {
        return LongAverageCalculator::new;
    }
}
