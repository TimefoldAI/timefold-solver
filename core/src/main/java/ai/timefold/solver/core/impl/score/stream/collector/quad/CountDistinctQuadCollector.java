package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.LongDistinctCountCalculator;

import org.jspecify.annotations.NonNull;

final class CountDistinctQuadCollector<A, B, C, D, Mapped_>
        extends
        ObjectCalculatorQuadCollector<A, B, C, D, Mapped_, Long, LongDistinctCountCalculator.State<Mapped_>, LongDistinctCountCalculator<Mapped_>> {
    CountDistinctQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper) {
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
