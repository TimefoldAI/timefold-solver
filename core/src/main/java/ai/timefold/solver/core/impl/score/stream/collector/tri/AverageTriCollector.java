package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLongAverageSlot;

import org.jspecify.annotations.NonNull;

final class AverageTriCollector<A, B, C>
        extends AbstractPrimitiveBasedTriCollector<A, B, C, Double, AbstractLongAverageSlot.State> {
    AverageTriCollector(ToLongTriFunction<? super A, ? super B, ? super C> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<AbstractLongAverageSlot.State> supplier() {
        return AbstractLongAverageSlot.State::new;
    }

    @Override
    public @NonNull Function<AbstractLongAverageSlot.State, Double> finisher() {
        return AbstractLongAverageSlot.State::result;
    }

    @Override
    protected TriConstraintCollectorValueHandle<A, B, C> newAccumulatedValue(AbstractLongAverageSlot.State state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractLongAverageSlot
            implements TriConstraintCollectorValueHandle<A, B, C> {
        Slot(AbstractLongAverageSlot.State state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c) {
            addMapped(mapper.applyAsLong(a, b, c));
        }

        @Override
        public void replaceWith(A a, B b, C c) {
            replaceWithMapped(mapper.applyAsLong(a, b, c));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
