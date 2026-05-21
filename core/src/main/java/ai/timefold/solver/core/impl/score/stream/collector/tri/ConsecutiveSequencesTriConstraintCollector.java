package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractSequenceSlot;

import org.jspecify.annotations.NonNull;

final class ConsecutiveSequencesTriConstraintCollector<A, B, C, Result_>
        extends
        AbstractReferenceBasedTriCollector<A, B, C, Result_, SequenceChain<Result_, Integer>, AbstractSequenceSlot.State<Result_>> {

    private final ToIntFunction<Result_> indexMap;

    public ConsecutiveSequencesTriConstraintCollector(TriFunction<A, B, C, Result_> resultMap,
            ToIntFunction<Result_> indexMap) {
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
    protected TriConstraintCollectorValueHandle<A, B, C> newAccumulatedValue(
            AbstractSequenceSlot.State<Result_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractSequenceSlot<Result_>
            implements TriConstraintCollectorValueHandle<A, B, C> {
        Slot(AbstractSequenceSlot.State<Result_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c) {
            addMapped(mapper.apply(a, b, c));
        }

        @Override
        public void replaceWith(A a, B b, C c) {
            replaceWithMapped(mapper.apply(a, b, c));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ConsecutiveSequencesTriConstraintCollector<?, ?, ?, ?> other) {
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
