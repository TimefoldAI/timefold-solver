package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.ConsecutiveSetTree;

final class ConsecutiveSequencesUniConstraintCollector<A> implements
        UniConstraintCollector<A, ConsecutiveSetTree<A, Integer, Integer>, ConstraintCollectors.SequenceChain<A, Integer>> {

    private final ToIntFunction<A> indexMap;

    public ConsecutiveSequencesUniConstraintCollector(ToIntFunction<A> indexMap) {
        this.indexMap = Objects.requireNonNull(indexMap);
    }

    @Override
    public Supplier<ConsecutiveSetTree<A, Integer, Integer>> supplier() {
        return () -> new ConsecutiveSetTree<>(
                (Integer a, Integer b) -> b - a,
                Integer::sum,
                1, 0);
    }

    @Override
    public BiFunction<ConsecutiveSetTree<A, Integer, Integer>, A, Runnable> accumulator() {
        return (acc, a) -> {
            Integer value = indexMap.applyAsInt(a);
            acc.add(a, value);
            return () -> acc.remove(a);
        };
    }

    @Override
    public Function<ConsecutiveSetTree<A, Integer, Integer>, ConstraintCollectors.SequenceChain<A, Integer>> finisher() {
        return tree -> tree;
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
