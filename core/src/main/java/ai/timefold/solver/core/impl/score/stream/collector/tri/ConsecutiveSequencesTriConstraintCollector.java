package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.impl.score.stream.collector.SequenceCalculator;

final class ConsecutiveSequencesTriConstraintCollector<A, B, C, Result_>
        extends
        ObjectCalculatorTriCollector<A, B, C, Result_, SequenceChain<Result_, Integer>, Result_, SequenceCalculator<Result_>> {

    private final ToIntFunction<Result_> indexMap;

    public ConsecutiveSequencesTriConstraintCollector(TriFunction<A, B, C, Result_> resultMap,
            ToIntFunction<Result_> indexMap) {
        super(resultMap);
        this.indexMap = Objects.requireNonNull(indexMap);
    }

    @Override
    public Supplier<SequenceCalculator<Result_>> supplier() {
        return () -> new SequenceCalculator<>(indexMap);
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
