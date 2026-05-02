package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractReferenceAverageSlot;

import org.jspecify.annotations.NonNull;

final class AverageReferenceBiCollector<A, B, Mapped_, Average_>
        extends
        ObjectCalculatorBiCollector<A, B, Mapped_, Average_, AbstractReferenceAverageSlot.State<Mapped_, Average_>> {
    private final Supplier<AbstractReferenceAverageSlot.State<Mapped_, Average_>> stateSupplier;

    AverageReferenceBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper,
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
    protected BiConstraintCollectorAccumulatedValue<A, B> newAccumulatedValue(
            AbstractReferenceAverageSlot.State<Mapped_, Average_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractReferenceAverageSlot<Mapped_, Average_>
            implements BiConstraintCollectorAccumulatedValue<A, B> {
        Slot(AbstractReferenceAverageSlot.State<Mapped_, Average_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b) {
            addMapped(mapper.apply(a, b));
        }

        @Override
        public void update(A a, B b) {
            updateMapped(mapper.apply(a, b));
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
        AverageReferenceBiCollector<?, ?, ?, ?> that = (AverageReferenceBiCollector<?, ?, ?, ?>) object;
        return Objects.equals(stateSupplier, that.stateSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), stateSupplier);
    }
}
