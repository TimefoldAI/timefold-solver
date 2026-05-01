package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.impl.score.stream.collector.LongAverageCalculator;

import org.jspecify.annotations.NonNull;

final class AverageTriCollector<A, B, C>
        extends LongCalculatorTriCollector<A, B, C, Double, LongAverageCalculator.State, LongAverageCalculator> {
    AverageTriCollector(ToLongTriFunction<? super A, ? super B, ? super C> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<LongAverageCalculator.State> supplier() {
        return LongAverageCalculator.State::new;
    }

    @Override
    public @NonNull Function<LongAverageCalculator.State, Double> finisher() {
        return LongAverageCalculator.State::result;
    }

    @Override
    protected LongAverageCalculator newCalculator(LongAverageCalculator.State state) {
        return new LongAverageCalculator(state);
    }
}
