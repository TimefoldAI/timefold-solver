package ai.timefold.solver.core.impl.score.stream.bi;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.IntDistinctCountCalculator;

final class CountDistinctIntBiCollector<A, B, Mapped_>
        extends ObjectCalculatorBiCollector<A, B, Mapped_, Integer, IntDistinctCountCalculator<Mapped_>> {
    CountDistinctIntBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntDistinctCountCalculator<Mapped_>> supplier() {
        return IntDistinctCountCalculator::new;
    }
}
