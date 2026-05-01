package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.collector.LongDistinctCountCalculator;

import org.jspecify.annotations.NonNull;

final class CountDistinctTriCollector<A, B, C, Mapped_>
        extends
        ObjectCalculatorTriCollector<A, B, C, Mapped_, Long, LongDistinctCountCalculator.State<Mapped_>, LongDistinctCountCalculator<Mapped_>> {
    CountDistinctTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<LongDistinctCountCalculator.State<Mapped_>> supplier() {
        return LongDistinctCountCalculator.State::new;
    }

    @Override
    public @NonNull Function<LongDistinctCountCalculator.State<Mapped_>, Long> finisher() {
        return LongDistinctCountCalculator.State::result;
    }

    @Override
    protected LongDistinctCountCalculator<Mapped_> newCalculator(LongDistinctCountCalculator.State<Mapped_> state) {
        return new LongDistinctCountCalculator<>(state);
    }
}
