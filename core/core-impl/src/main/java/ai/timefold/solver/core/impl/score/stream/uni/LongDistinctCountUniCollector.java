package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.LongDistinctCountCalculator;

public final class LongDistinctCountUniCollector<A, Mapped_>
        extends ObjectCalculatorUniCollector<A, Mapped_, Long, LongDistinctCountCalculator<Mapped_>> {
    public LongDistinctCountUniCollector(Function<? super A, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongDistinctCountCalculator<Mapped_>> supplier() {
        return LongDistinctCountCalculator::new;
    }
}
