package ai.timefold.solver.core.impl.score.stream.bi;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.LongDistinctCountCalculator;

public final class LongDistinctCountBiCollector<A, B, Mapped_>
        extends ObjectCalculatorBiCollector<A, B, Mapped_, Long, LongDistinctCountCalculator<Mapped_>> {
    public LongDistinctCountBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongDistinctCountCalculator<Mapped_>> supplier() {
        return LongDistinctCountCalculator::new;
    }
}
