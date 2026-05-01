package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.impl.score.stream.collector.SequenceCalculator;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

import org.jspecify.annotations.NonNull;

final class ConsecutiveSequencesUniConstraintCollector<A>
        extends
        ObjectCalculatorUniCollector<A, A, SequenceChain<A, Integer>, SequenceCalculator.State<A>, SequenceCalculator<A>> {

    private final ToIntFunction<A> indexMap;

    public ConsecutiveSequencesUniConstraintCollector(ToIntFunction<A> indexMap) {
        super(ConstantLambdaUtils.identity());
        this.indexMap = Objects.requireNonNull(indexMap);
    }

    @Override
    protected SequenceCalculator<A> newCalculator(SequenceCalculator.State<A> state) {
        return new SequenceCalculator<>(state);
    }

    @Override
    public @NonNull Supplier<SequenceCalculator.State<A>> supplier() {
        return () -> new SequenceCalculator.State<>(indexMap);
    }

    @Override
    public @NonNull Function<SequenceCalculator.State<A>, SequenceChain<A, Integer>> finisher() {
        return SequenceCalculator.State::result;
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
