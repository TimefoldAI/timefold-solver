package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.IntDistinctCountCalculator;

public final class IntDistinctCountUniCollector<A, Mapped_>
        extends ObjectCalculatorUniCollector<A, Mapped_, Integer, IntDistinctCountCalculator<Mapped_>> {
    public IntDistinctCountUniCollector(Function<? super A, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<IntDistinctCountCalculator<Mapped_>> supplier() {
        return IntDistinctCountCalculator::new;
    }
}
