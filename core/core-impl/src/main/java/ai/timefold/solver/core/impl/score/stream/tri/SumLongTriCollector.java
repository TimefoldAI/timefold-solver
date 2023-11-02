package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.impl.score.stream.LongSumCalculator;

public final class SumLongTriCollector<A, B, C> extends LongCalculatorTriCollector<A, B, C, Long, LongSumCalculator> {
    private SumLongTriCollector(ToLongTriFunction<? super A, ? super B, ? super C> mapper) {
        super(mapper);
    }

    public static <A, B, C> SumLongTriCollector<A, B, C> sum(ToLongTriFunction<? super A, ? super B, ? super C> mapper) {
        return new SumLongTriCollector<A, B, C>(mapper);
    }

    @Override
    public Supplier<LongSumCalculator> supplier() {
        return LongSumCalculator::new;
    }
}
