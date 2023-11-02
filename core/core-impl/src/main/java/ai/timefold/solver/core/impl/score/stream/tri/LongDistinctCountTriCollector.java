package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.LongDistinctCountCalculator;

public final class LongDistinctCountTriCollector<A, B, C, Mapped_>
        extends ObjectCalculatorTriCollector<A, B, C, Mapped_, Long, LongDistinctCountCalculator<Mapped_>> {
    public LongDistinctCountTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongDistinctCountCalculator<Mapped_>> supplier() {
        return LongDistinctCountCalculator::new;
    }
}
