package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.LongDistinctCountCalculator;

public final class CountDistinctLongUniCollector<A, Mapped_>
        extends ObjectCalculatorUniCollector<A, Mapped_, Long, LongDistinctCountCalculator<Mapped_>> {
    CountDistinctLongUniCollector(Function<? super A, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<LongDistinctCountCalculator<Mapped_>> supplier() {
        return LongDistinctCountCalculator::new;
    }
}
