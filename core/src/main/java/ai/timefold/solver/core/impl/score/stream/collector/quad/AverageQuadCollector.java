package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractLongAverageSlot;

import org.jspecify.annotations.NonNull;

final class AverageQuadCollector<A, B, C, D>
        extends AbstractPrimitiveBasedQuadCollector<A, B, C, D, Double, AbstractLongAverageSlot.State> {
    AverageQuadCollector(ToLongQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
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
    protected QuadConstraintCollectorValueHandle<A, B, C, D> newAccumulatedValue(AbstractLongAverageSlot.State state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractLongAverageSlot
            implements QuadConstraintCollectorValueHandle<A, B, C, D> {
        Slot(AbstractLongAverageSlot.State state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c, D d) {
            addMapped(mapper.applyAsLong(a, b, c, d));
        }

        @Override
        public void replaceWith(A a, B b, C c, D d) {
            replaceWithMapped(mapper.applyAsLong(a, b, c, d));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
