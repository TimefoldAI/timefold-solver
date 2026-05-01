package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.impl.score.stream.collector.LongAverageCalculator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class AverageUniCollector<A>
        extends LongCalculatorUniCollector<A, Double, LongAverageCalculator.State, LongAverageCalculator> {
    AverageUniCollector(ToLongFunction<? super A> mapper) {
        super(mapper);
    }

    @Override
    protected LongAverageCalculator newCalculator(LongAverageCalculator.State state) {
        return new LongAverageCalculator(state);
    }

    @Override
    public @NonNull Supplier<LongAverageCalculator.State> supplier() {
        return LongAverageCalculator.State::new;
    }

    @Override
    public @NonNull Function<LongAverageCalculator.State, @Nullable Double> finisher() {
        return LongAverageCalculator.State::result;
    }
}
