package ai.timefold.solver.core.impl.bavet.quad.collector;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.bavet.common.collector.IntDistinctCountCalculator;

import org.jspecify.annotations.NonNull;

final class CountDistinctIntQuadCollector<A, B, C, D, Mapped_>
        extends ObjectCalculatorQuadCollector<A, B, C, D, Mapped_, Integer, Mapped_, IntDistinctCountCalculator<Mapped_>> {
    CountDistinctIntQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<IntDistinctCountCalculator<Mapped_>> supplier() {
        return IntDistinctCountCalculator::new;
    }
}
