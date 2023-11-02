package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.IntDistinctCountCalculator;

public final class IntDistinctCountQuadCollector<A, B, C, D, Mapped_>
        extends ObjectCalculatorQuadCollector<A, B, C, D, Mapped_, Integer, IntDistinctCountCalculator<Mapped_>> {
    public IntDistinctCountQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntDistinctCountCalculator<Mapped_>> supplier() {
        return IntDistinctCountCalculator::new;
    }
}
