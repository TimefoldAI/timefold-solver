package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.LongDistinctCountCalculator;

import org.jspecify.annotations.NonNull;

final class CountDistinctUniCollector<A, Mapped_>
        extends
        ObjectCalculatorUniCollector<A, Mapped_, Long, LongDistinctCountCalculator.State<Mapped_>, LongDistinctCountCalculator<Mapped_>> {
    CountDistinctUniCollector(Function<? super A, ? extends Mapped_> mapper) {
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
