package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.LongDistinctCountCalculator;

final class CountDistinctLongQuadCollector<A, B, C, D, Mapped_>
        extends ObjectCalculatorQuadCollector<A, B, C, D, Mapped_, Long, Mapped_, LongDistinctCountCalculator<Mapped_>> {
    CountDistinctLongQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongDistinctCountCalculator<Mapped_>> supplier() {
        return LongDistinctCountCalculator::new;
    }
}
