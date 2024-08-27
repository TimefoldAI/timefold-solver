package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.IntDistinctCountCalculator;

final class CountDistinctIntUniCollector<A, Mapped_>
        extends ObjectCalculatorUniCollector<A, Mapped_, Integer, Mapped_, IntDistinctCountCalculator<Mapped_>> {
    CountDistinctIntUniCollector(Function<? super A, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntDistinctCountCalculator<Mapped_>> supplier() {
        return IntDistinctCountCalculator::new;
    }
}
