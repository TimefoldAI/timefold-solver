package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.impl.score.stream.collector.LongSumCalculator;

import org.jspecify.annotations.NonNull;

final class SumTriCollector<A, B, C>
        extends LongCalculatorTriCollector<A, B, C, Long, LongSumCalculator.State, LongSumCalculator> {
    SumTriCollector(ToLongTriFunction<? super A, ? super B, ? super C> mapper) {
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
