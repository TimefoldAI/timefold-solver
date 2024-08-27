package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.collector.IntDistinctCountCalculator;

final class CountDistinctIntTriCollector<A, B, C, Mapped_>
        extends ObjectCalculatorTriCollector<A, B, C, Mapped_, Integer, Mapped_, IntDistinctCountCalculator<Mapped_>> {
    CountDistinctIntTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntDistinctCountCalculator<Mapped_>> supplier() {
        return IntDistinctCountCalculator::new;
    }
}
