package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractSequenceSlot;

import org.jspecify.annotations.NonNull;

final class ConsecutiveSequencesQuadConstraintCollector<A, B, C, D, Result_>
        extends
        ObjectCalculatorQuadCollector<A, B, C, D, Result_, SequenceChain<Result_, Integer>, AbstractSequenceSlot.State<Result_>> {

    private final ToIntFunction<Result_> indexMap;

    public ConsecutiveSequencesQuadConstraintCollector(QuadFunction<A, B, C, D, Result_> resultMap,
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
    protected QuadConstraintCollectorValueHandle<A, B, C, D> newAccumulatedValue(
            AbstractSequenceSlot.State<Result_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractSequenceSlot<Result_>
            implements QuadConstraintCollectorValueHandle<A, B, C, D> {
        Slot(AbstractSequenceSlot.State<Result_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c, D d) {
            addMapped(mapper.apply(a, b, c, d));
        }

        @Override
        public void replaceWith(A a, B b, C c, D d) {
            replaceWithMapped(mapper.apply(a, b, c, d));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ConsecutiveSequencesQuadConstraintCollector<?, ?, ?, ?, ?> other) {
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
