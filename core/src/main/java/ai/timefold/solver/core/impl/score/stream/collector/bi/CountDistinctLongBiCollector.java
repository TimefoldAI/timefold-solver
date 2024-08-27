package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.LongDistinctCountCalculator;

final class CountDistinctLongBiCollector<A, B, Mapped_>
        extends ObjectCalculatorBiCollector<A, B, Mapped_, Long, Mapped_, LongDistinctCountCalculator<Mapped_>> {
    CountDistinctLongBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongDistinctCountCalculator<Mapped_>> supplier() {
        return LongDistinctCountCalculator::new;
    }
}
