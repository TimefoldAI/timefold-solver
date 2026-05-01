package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.LongSumCalculator;

import org.jspecify.annotations.NonNull;

final class SumQuadCollector<A, B, C, D>
        extends LongCalculatorQuadCollector<A, B, C, D, Long, LongSumCalculator.State, LongSumCalculator> {
    SumQuadCollector(ToLongQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<LongSumCalculator.State> supplier() {
        return LongSumCalculator.State::new;
    }

    @Override
    public @NonNull Function<LongSumCalculator.State, Long> finisher() {
        return LongSumCalculator.State::result;
    }

    @Override
    protected LongSumCalculator newCalculator(LongSumCalculator.State state) {
        return new LongSumCalculator(state);
    }
}
