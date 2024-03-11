package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.impl.score.stream.collector.IntAverageCalculator;

final class AverageIntUniCollector<A> extends IntCalculatorUniCollector<A, Double, IntAverageCalculator> {
    AverageIntUniCollector(ToIntFunction<? super A> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntAverageCalculator> supplier() {
        return IntAverageCalculator::new;
    }
}
