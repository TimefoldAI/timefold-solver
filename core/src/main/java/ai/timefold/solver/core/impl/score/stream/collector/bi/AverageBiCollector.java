package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.impl.score.stream.collector.LongAverageCalculator;

import org.jspecify.annotations.NonNull;

final class AverageBiCollector<A, B>
        extends LongCalculatorBiCollector<A, B, Double, LongAverageCalculator.State, LongAverageCalculator> {
    AverageBiCollector(ToLongBiFunction<? super A, ? super B> mapper) {
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
    public @NonNull Function<LongAverageCalculator.State, Double> finisher() {
        return LongAverageCalculator.State::result;
    }
}
