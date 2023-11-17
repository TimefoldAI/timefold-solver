package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.ConsecutiveSetTree;
import ai.timefold.solver.core.impl.score.stream.SequenceAccumulator;

final class ConsecutiveSequencesQuadConstraintCollector<A, B, C, D, Result_>
        implements
        QuadConstraintCollector<A, B, C, D, ConsecutiveSetTree<Result_, Integer, Integer>, ConstraintCollectors.SequenceChain<Result_, Integer>> {

    private final QuadFunction<A, B, C, D, Result_> resultMap;
    private final SequenceAccumulator<Result_> accumulator;

    public ConsecutiveSequencesQuadConstraintCollector(QuadFunction<A, B, C, D, Result_> resultMap,
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
    public PentaFunction<ConsecutiveSetTree<Result_, Integer, Integer>, A, B, C, D, Runnable> accumulator() {
        return (acc, a, b, c, d) -> {
            Result_ result = resultMap.apply(a, b, c, d);
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
        if (o instanceof ConsecutiveSequencesQuadConstraintCollector<?, ?, ?, ?, ?> other) {
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
