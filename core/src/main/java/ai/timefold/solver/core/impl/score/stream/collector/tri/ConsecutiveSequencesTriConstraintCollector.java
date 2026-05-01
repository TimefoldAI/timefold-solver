package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.impl.score.stream.collector.SequenceCalculator;

import org.jspecify.annotations.NonNull;

final class ConsecutiveSequencesTriConstraintCollector<A, B, C, Result_>
        extends
        ObjectCalculatorTriCollector<A, B, C, Result_, SequenceChain<Result_, Integer>, SequenceCalculator.State<Result_>, SequenceCalculator<Result_>> {

    private final ToIntFunction<Result_> indexMap;

    public ConsecutiveSequencesTriConstraintCollector(TriFunction<A, B, C, Result_> resultMap,
            ToIntFunction<Result_> indexMap) {
        super(resultMap);
        this.indexMap = Objects.requireNonNull(indexMap);
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
    protected SequenceCalculator<Result_> newCalculator(SequenceCalculator.State<Result_> state) {
        return new SequenceCalculator<>(state);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ConsecutiveSequencesTriConstraintCollector<?, ?, ?, ?> other) {
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
