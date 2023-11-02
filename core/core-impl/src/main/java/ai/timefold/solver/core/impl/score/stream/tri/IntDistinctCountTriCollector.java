package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.IntDistinctCountCalculator;

public final class IntDistinctCountTriCollector<A, B, C, Mapped_>
        extends ObjectCalculatorTriCollector<A, B, C, Mapped_, Integer, IntDistinctCountCalculator<Mapped_>> {
    public IntDistinctCountTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntDistinctCountCalculator<Mapped_>> supplier() {
        return IntDistinctCountCalculator::new;
    }
}
