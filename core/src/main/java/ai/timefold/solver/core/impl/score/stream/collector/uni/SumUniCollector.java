package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.impl.score.stream.collector.LongSumCalculator;

import org.jspecify.annotations.NonNull;

final class SumUniCollector<A> extends LongCalculatorUniCollector<A, Long, LongSumCalculator.State, LongSumCalculator> {
    SumUniCollector(ToLongFunction<? super A> mapper) {
        super(mapper);
    }

    @Override
    protected LongSumCalculator newCalculator(LongSumCalculator.State state) {
        return new LongSumCalculator(state);
    }

    @Override
    public @NonNull Supplier<LongSumCalculator.State> supplier() {
        return LongSumCalculator.State::new;
    }

    @Override
    public @NonNull Function<LongSumCalculator.State, Long> finisher() {
        return LongSumCalculator.State::result;
    }
}
