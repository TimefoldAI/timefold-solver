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
import ai.timefold.solver.core.impl.score.stream.SequenceAccumulator;

final class ConsecutiveSequencesBiConstraintCollector<A, B, Result_>
        implements
        BiConstraintCollector<A, B, ConsecutiveSetTree<Result_, Integer, Integer>, ConstraintCollectors.SequenceChain<Result_, Integer>> {

    private final BiFunction<A, B, Result_> resultMap;
    private final SequenceAccumulator<Result_> accumulator;

    public ConsecutiveSequencesBiConstraintCollector(BiFunction<A, B, Result_> resultMap, ToIntFunction<Result_> indexMap) {
        this.resultMap = Objects.requireNonNull(resultMap);
        this.accumulator = new SequenceAccumulator<>(indexMap);
    }

    @Override
    public Supplier<ConsecutiveSetTree<Result_, Integer, Integer>> supplier() {
        return accumulator.getContextSupplier();
    }

    @Override
    public TriFunction<ConsecutiveSetTree<Result_, Integer, Integer>, A, B, Runnable> accumulator() {
        return (acc, a, b) -> {
            Result_ result = resultMap.apply(a, b);
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
        if (o instanceof ConsecutiveSequencesBiConstraintCollector<?, ?, ?> other) {
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
