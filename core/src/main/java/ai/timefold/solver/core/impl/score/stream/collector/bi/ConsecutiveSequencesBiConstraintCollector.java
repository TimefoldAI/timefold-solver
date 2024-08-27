package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.impl.score.stream.collector.SequenceCalculator;

final class ConsecutiveSequencesBiConstraintCollector<A, B, Result_>
        extends
        ObjectCalculatorBiCollector<A, B, Result_, SequenceChain<Result_, Integer>, Result_, SequenceCalculator<Result_>> {

    private final ToIntFunction<Result_> indexMap;

    public ConsecutiveSequencesBiConstraintCollector(BiFunction<A, B, Result_> resultMap, ToIntFunction<Result_> indexMap) {
        super(resultMap);
        this.indexMap = Objects.requireNonNull(indexMap);
    }

    @Override
    public Supplier<SequenceCalculator<Result_>> supplier() {
        return () -> new SequenceCalculator<>(indexMap);
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
