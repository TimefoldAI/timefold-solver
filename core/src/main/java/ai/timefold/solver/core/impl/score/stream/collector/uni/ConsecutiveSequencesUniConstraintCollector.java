package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractSequenceSlot;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

import org.jspecify.annotations.NonNull;

final class ConsecutiveSequencesUniConstraintCollector<A>
        extends ObjectCalculatorUniCollector<A, A, SequenceChain<A, Integer>, AbstractSequenceSlot.State<A>> {

    private final ToIntFunction<A> indexMap;

    public ConsecutiveSequencesUniConstraintCollector(ToIntFunction<A> indexMap) {
        super(ConstantLambdaUtils.identity());
        this.indexMap = Objects.requireNonNull(indexMap);
    }

    @Override
    public @NonNull Supplier<AbstractSequenceSlot.State<A>> supplier() {
        return () -> new AbstractSequenceSlot.State<>(indexMap);
    }

    @Override
    public @NonNull Function<AbstractSequenceSlot.State<A>, SequenceChain<A, Integer>> finisher() {
        return AbstractSequenceSlot.State::result;
    }

    @Override
    protected UniConstraintCollectorValueHandle<A>
            newAccumulatedValue(AbstractSequenceSlot.State<A> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractSequenceSlot<A>
            implements UniConstraintCollectorValueHandle<A> {
        Slot(AbstractSequenceSlot.State<A> state) {
            super(state);
        }

        @Override
        public void add(A a) {
            addMapped(mapper.apply(a));
        }

        @Override
        public void update(A a) {
            updateMapped(mapper.apply(a));
        }

        @Override
        public void remove() {
            removeMapped();
        }
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
