package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.LongDistinctCountCalculator;

import org.jspecify.annotations.NonNull;

final class CountDistinctLongUniCollector<A, Mapped_>
        extends ObjectCalculatorUniCollector<A, Mapped_, Long, Mapped_, LongDistinctCountCalculator<Mapped_>> {
    CountDistinctLongUniCollector(Function<? super A, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<LongDistinctCountCalculator<Mapped_>> supplier() {
        return LongDistinctCountCalculator::new;
    }
}
