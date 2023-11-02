package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.impl.score.stream.IntSumCalculator;

public final class SumIntUniCollector<A> extends IntCalculatorUniCollector<A, Integer, IntSumCalculator> {
    SumIntUniCollector(ToIntFunction<? super A> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntSumCalculator> supplier() {
        return IntSumCalculator::new;
    }
}
