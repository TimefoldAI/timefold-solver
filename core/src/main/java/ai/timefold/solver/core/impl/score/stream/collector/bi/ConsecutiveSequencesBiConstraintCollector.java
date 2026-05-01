package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.impl.score.stream.collector.SequenceCalculator;

import org.jspecify.annotations.NonNull;

final class ConsecutiveSequencesBiConstraintCollector<A, B, Result_>
        extends
        ObjectCalculatorBiCollector<A, B, Result_, SequenceChain<Result_, Integer>, SequenceCalculator.State<Result_>, SequenceCalculator<Result_>> {

    private final ToIntFunction<Result_> indexMap;

    public ConsecutiveSequencesBiConstraintCollector(BiFunction<A, B, Result_> resultMap, ToIntFunction<Result_> indexMap) {
        super(resultMap);
        this.indexMap = Objects.requireNonNull(indexMap);
    }

    @Override
    protected SequenceCalculator<Result_> newCalculator(SequenceCalculator.State<Result_> state) {
        return new SequenceCalculator<>(state);
    }

    @Override
    public @NonNull Supplier<SequenceCalculator.State<Result_>> supplier() {
        return () -> new SequenceCalculator.State<>(indexMap);
    }

    @Override
    public @NonNull Function<SequenceCalculator.State<Result_>, SequenceChain<Result_, Integer>> finisher() {
        return SequenceCalculator.State::result;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ConsecutiveSequencesBiConstraintCollector<?, ?, ?> other) {
            return Objects.equals(mapper, other.mapper)
                    && Objects.equals(indexMap, other.indexMap);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper, indexMap);
    }
}
