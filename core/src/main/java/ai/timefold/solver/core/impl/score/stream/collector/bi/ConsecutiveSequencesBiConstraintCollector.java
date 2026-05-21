package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorValueHandle;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractSequenceSlot;

import org.jspecify.annotations.NonNull;

final class ConsecutiveSequencesBiConstraintCollector<A, B, Result_>
        extends
        AbstractReferenceBasedBiCollector<A, B, Result_, SequenceChain<Result_, Integer>, AbstractSequenceSlot.State<Result_>> {

    private final ToIntFunction<Result_> indexMap;

    public ConsecutiveSequencesBiConstraintCollector(BiFunction<A, B, Result_> resultMap, ToIntFunction<Result_> indexMap) {
        super(resultMap);
        this.indexMap = Objects.requireNonNull(indexMap);
    }

    @Override
    public @NonNull Supplier<AbstractSequenceSlot.State<Result_>> supplier() {
        return () -> new AbstractSequenceSlot.State<>(indexMap);
    }

    @Override
    public @NonNull Function<AbstractSequenceSlot.State<Result_>, SequenceChain<Result_, Integer>> finisher() {
        return AbstractSequenceSlot.State::result;
    }

    @Override
    protected BiConstraintCollectorValueHandle<A, B> newAccumulatedValue(AbstractSequenceSlot.State<Result_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractSequenceSlot<Result_>
            implements BiConstraintCollectorValueHandle<A, B> {
        Slot(AbstractSequenceSlot.State<Result_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b) {
            addMapped(mapper.apply(a, b));
        }

        @Override
        public void replaceWith(A a, B b) {
            replaceWithMapped(mapper.apply(a, b));
        }

        @Override
        public void remove() {
            removeMapped();
        }
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
