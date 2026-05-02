package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractReferenceAverageSlot;

import org.jspecify.annotations.NonNull;

final class AverageReferenceTriCollector<A, B, C, Mapped_, Average_>
        extends
        ObjectCalculatorTriCollector<A, B, C, Mapped_, Average_, AbstractReferenceAverageSlot.State<Mapped_, Average_>> {
    private final Supplier<AbstractReferenceAverageSlot.State<Mapped_, Average_>> stateSupplier;

    AverageReferenceTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper,
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
    protected TriConstraintCollectorAccumulatedValue<A, B, C> newAccumulatedValue(
            AbstractReferenceAverageSlot.State<Mapped_, Average_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractReferenceAverageSlot<Mapped_, Average_>
            implements TriConstraintCollectorAccumulatedValue<A, B, C> {
        Slot(AbstractReferenceAverageSlot.State<Mapped_, Average_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c) {
            addMapped(mapper.apply(a, b, c));
        }

        @Override
        public void update(A a, B b, C c) {
            updateMapped(mapper.apply(a, b, c));
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
        AverageReferenceTriCollector<?, ?, ?, ?, ?> that = (AverageReferenceTriCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(stateSupplier, that.stateSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), stateSupplier);
    }
}
