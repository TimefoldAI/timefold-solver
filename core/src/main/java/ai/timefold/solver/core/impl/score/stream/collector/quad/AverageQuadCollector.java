package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.LongAverageCalculator;

import org.jspecify.annotations.NonNull;

final class AverageQuadCollector<A, B, C, D>
        extends LongCalculatorQuadCollector<A, B, C, D, Double, LongAverageCalculator.State, LongAverageCalculator> {
    AverageQuadCollector(ToLongQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
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
