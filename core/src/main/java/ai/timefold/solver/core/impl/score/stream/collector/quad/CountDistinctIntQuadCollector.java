package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.IntDistinctCountCalculator;

final class CountDistinctIntQuadCollector<A, B, C, D, Mapped_>
        extends ObjectCalculatorQuadCollector<A, B, C, D, Mapped_, Integer, Mapped_, IntDistinctCountCalculator<Mapped_>> {
    CountDistinctIntQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntDistinctCountCalculator<Mapped_>> supplier() {
        return IntDistinctCountCalculator::new;
    }
}
