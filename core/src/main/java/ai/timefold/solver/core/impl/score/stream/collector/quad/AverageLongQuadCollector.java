package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.LongAverageCalculator;

import org.jspecify.annotations.NonNull;

final class AverageLongQuadCollector<A, B, C, D>
        extends LongCalculatorQuadCollector<A, B, C, D, Double, LongAverageCalculator> {
    AverageLongQuadCollector(ToLongQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<LongAverageCalculator> supplier() {
        return LongAverageCalculator::new;
    }
}
