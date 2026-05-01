package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.LongDistinctCountCalculator;

import org.jspecify.annotations.NonNull;

final class CountDistinctBiCollector<A, B, Mapped_>
        extends
        ObjectCalculatorBiCollector<A, B, Mapped_, Long, LongDistinctCountCalculator.State<Mapped_>, LongDistinctCountCalculator<Mapped_>> {
    CountDistinctBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    protected LongDistinctCountCalculator<Mapped_> newCalculator(LongDistinctCountCalculator.State<Mapped_> state) {
        return new LongDistinctCountCalculator<>(state);
    }

    @Override
    public @NonNull Supplier<LongDistinctCountCalculator.State<Mapped_>> supplier() {
        return LongDistinctCountCalculator.State::new;
    }

    @Override
    public @NonNull Function<LongDistinctCountCalculator.State<Mapped_>, Long> finisher() {
        return LongDistinctCountCalculator.State::result;
    }
}
