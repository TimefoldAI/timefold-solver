package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.impl.score.stream.IntAverageCalculator;

public final class AverageIntUniCollector<A> extends IntCalculatorUniCollector<A, Double, IntAverageCalculator> {
    AverageIntUniCollector(ToIntFunction<? super A> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntAverageCalculator> supplier() {
        return IntAverageCalculator::new;
    }
}
