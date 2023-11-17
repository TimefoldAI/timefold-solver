package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.ConsecutiveSetTree;
import ai.timefold.solver.core.impl.score.stream.SequenceAccumulator;

final class ConsecutiveSequencesTriConstraintCollector<A, B, C, Result_>
        implements
        TriConstraintCollector<A, B, C, ConsecutiveSetTree<Result_, Integer, Integer>, ConstraintCollectors.SequenceChain<Result_, Integer>> {

    private final TriFunction<A, B, C, Result_> resultMap;
    private final SequenceAccumulator<Result_> accumulator;

    public ConsecutiveSequencesTriConstraintCollector(TriFunction<A, B, C, Result_> resultMap,
            ToIntFunction<Result_> indexMap) {
        this.resultMap = Objects.requireNonNull(resultMap);
        this.accumulator = new SequenceAccumulator<>(indexMap);
    }

    @Override
    public Supplier<ConsecutiveSetTree<Result_, Integer, Integer>> supplier() {
        return () -> new ConsecutiveSetTree<>(
                (Integer a, Integer b) -> b - a,
                Integer::sum, 1, 0);
    }

    @Override
    public QuadFunction<ConsecutiveSetTree<Result_, Integer, Integer>, A, B, C, Runnable> accumulator() {
        return (acc, a, b, c) -> {
            Result_ result = resultMap.apply(a, b, c);
            return accumulator.accumulate(acc, result);
        };
    }

    @Override
    public Function<ConsecutiveSetTree<Result_, Integer, Integer>, ConstraintCollectors.SequenceChain<Result_, Integer>>
            finisher() {
        return tree -> tree;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ConsecutiveSequencesTriConstraintCollector<?, ?, ?, ?> other) {
            return Objects.equals(resultMap, other.resultMap)
                    && Objects.equals(accumulator, other.accumulator);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resultMap, accumulator);
    }

}
