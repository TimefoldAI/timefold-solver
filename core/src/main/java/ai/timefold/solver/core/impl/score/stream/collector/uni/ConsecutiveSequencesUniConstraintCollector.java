package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.impl.score.stream.collector.SequenceCalculator;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

final class ConsecutiveSequencesUniConstraintCollector<A>
        extends ObjectCalculatorUniCollector<A, A, SequenceChain<A, Integer>, A, SequenceCalculator<A>> {

    private final ToIntFunction<A> indexMap;

    public ConsecutiveSequencesUniConstraintCollector(ToIntFunction<A> indexMap) {
        super(ConstantLambdaUtils.identity());
        this.indexMap = Objects.requireNonNull(indexMap);
    }

    @Override
    public Supplier<SequenceCalculator<A>> supplier() {
        return () -> new SequenceCalculator<>(indexMap);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ConsecutiveSequencesUniConstraintCollector<?> other) {
            return Objects.equals(indexMap, other.indexMap);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return indexMap.hashCode();
    }
}
