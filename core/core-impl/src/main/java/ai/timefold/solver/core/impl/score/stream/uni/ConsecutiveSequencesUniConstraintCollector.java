package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.ConsecutiveSetTree;
import ai.timefold.solver.core.impl.score.stream.SequenceAccumulator;

final class ConsecutiveSequencesUniConstraintCollector<A> implements
        UniConstraintCollector<A, ConsecutiveSetTree<A, Integer, Integer>, ConstraintCollectors.SequenceChain<A, Integer>> {

    private final SequenceAccumulator<A> accumulator;

    public ConsecutiveSequencesUniConstraintCollector(ToIntFunction<A> indexMap) {
        this.accumulator = new SequenceAccumulator<>(indexMap);
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
        return accumulator::accumulate;
    }

    @Override
    public Function<ConsecutiveSetTree<A, Integer, Integer>, ConstraintCollectors.SequenceChain<A, Integer>> finisher() {
        return tree -> tree;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ConsecutiveSequencesUniConstraintCollector<?> other) {
            return Objects.equals(accumulator, other.accumulator);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return accumulator.hashCode();
    }

}
