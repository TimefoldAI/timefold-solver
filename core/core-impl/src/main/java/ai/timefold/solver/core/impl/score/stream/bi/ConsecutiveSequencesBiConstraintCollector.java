package ai.timefold.solver.core.impl.score.stream.bi;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.ConsecutiveSetTree;

final class ConsecutiveSequencesBiConstraintCollector<A, B, Result_>
        implements
        BiConstraintCollector<A, B, ConsecutiveSetTree<Result_, Integer, Integer>, ConstraintCollectors.SequenceChain<Result_, Integer>> {

    private final BiFunction<A, B, Result_> resultMap;
    private final ToIntFunction<Result_> indexMap;

    public ConsecutiveSequencesBiConstraintCollector(BiFunction<A, B, Result_> resultMap, ToIntFunction<Result_> indexMap) {
        this.resultMap = Objects.requireNonNull(resultMap);
        this.indexMap = Objects.requireNonNull(indexMap);
    }

    @Override
    public Supplier<ConsecutiveSetTree<Result_, Integer, Integer>> supplier() {
        return () -> new ConsecutiveSetTree<>(
                (Integer a, Integer b) -> b - a,
                Integer::sum, 1, 0);
    }

    @Override
    public TriFunction<ConsecutiveSetTree<Result_, Integer, Integer>, A, B, Runnable> accumulator() {
        return (acc, a, b) -> {
            Result_ result = resultMap.apply(a, b);
            Integer value = indexMap.applyAsInt(result);
            acc.add(result, value);
            return () -> acc.remove(result);
        };
    }

    @Override
    public Function<ConsecutiveSetTree<Result_, Integer, Integer>, ConstraintCollectors.SequenceChain<Result_, Integer>>
            finisher() {
        return tree -> tree;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ConsecutiveSequencesBiConstraintCollector<?, ?, ?> other) {
            return Objects.equals(resultMap, other.resultMap)
                    && Objects.equals(indexMap, other.indexMap);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resultMap, indexMap);
    }

}
