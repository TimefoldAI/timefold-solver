package ai.timefold.solver.core.impl.bavet.tri.collector;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.bavet.common.collector.LongDistinctCountCalculator;

import org.jspecify.annotations.NonNull;

final class CountDistinctLongTriCollector<A, B, C, Mapped_>
        extends ObjectCalculatorTriCollector<A, B, C, Mapped_, Long, Mapped_, LongDistinctCountCalculator<Mapped_>> {
    CountDistinctLongTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<LongDistinctCountCalculator<Mapped_>> supplier() {
        return LongDistinctCountCalculator::new;
    }
}
