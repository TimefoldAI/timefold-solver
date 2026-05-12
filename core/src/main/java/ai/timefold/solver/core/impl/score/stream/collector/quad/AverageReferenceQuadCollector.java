package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractReferenceAverageSlot;

import org.jspecify.annotations.NonNull;

final class AverageReferenceQuadCollector<A, B, C, D, Mapped_, Average_>
        extends
        ObjectCalculatorQuadCollector<A, B, C, D, Mapped_, Average_, AbstractReferenceAverageSlot.State<Mapped_, Average_>> {
    private final Supplier<AbstractReferenceAverageSlot.State<Mapped_, Average_>> stateSupplier;

    AverageReferenceQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper,
            Supplier<AbstractReferenceAverageSlot.State<Mapped_, Average_>> stateSupplier) {
        super(mapper);
        this.stateSupplier = stateSupplier;
    }

    @Override
    public @NonNull Supplier<AbstractReferenceAverageSlot.State<Mapped_, Average_>> supplier() {
        return stateSupplier;
    }

    @Override
    public @NonNull Function<AbstractReferenceAverageSlot.State<Mapped_, Average_>, Average_> finisher() {
        return AbstractReferenceAverageSlot.State::result;
    }

    @Override
    protected QuadConstraintCollectorValueHandle<A, B, C, D> newAccumulatedValue(
            AbstractReferenceAverageSlot.State<Mapped_, Average_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractReferenceAverageSlot<Mapped_, Average_>
            implements QuadConstraintCollectorValueHandle<A, B, C, D> {
        Slot(AbstractReferenceAverageSlot.State<Mapped_, Average_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c, D d) {
            addMapped(mapper.apply(a, b, c, d));
        }

        @Override
        public void update(A a, B b, C c, D d) {
            updateMapped(mapper.apply(a, b, c, d));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        if (!super.equals(object))
            return false;
        AverageReferenceQuadCollector<?, ?, ?, ?, ?, ?> that = (AverageReferenceQuadCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(stateSupplier, that.stateSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), stateSupplier);
    }
}
