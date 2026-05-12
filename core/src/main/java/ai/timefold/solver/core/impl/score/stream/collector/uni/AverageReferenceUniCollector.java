package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractReferenceAverageSlot;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class AverageReferenceUniCollector<A, Mapped_, Average_>
        extends ObjectCalculatorUniCollector<A, Mapped_, Average_, AbstractReferenceAverageSlot.State<Mapped_, Average_>> {
    private final Supplier<AbstractReferenceAverageSlot.State<Mapped_, Average_>> stateSupplier;

    AverageReferenceUniCollector(Function<? super A, ? extends Mapped_> mapper,
            Supplier<AbstractReferenceAverageSlot.State<Mapped_, Average_>> stateSupplier) {
        super(mapper);
        this.stateSupplier = stateSupplier;
    }

    @Override
    public @NonNull Supplier<AbstractReferenceAverageSlot.State<Mapped_, Average_>> supplier() {
        return stateSupplier;
    }

    @Override
    public @NonNull Function<AbstractReferenceAverageSlot.State<Mapped_, Average_>, @Nullable Average_> finisher() {
        return AbstractReferenceAverageSlot.State::result;
    }

    @Override
    protected UniConstraintCollectorValueHandle<A>
            newAccumulatedValue(AbstractReferenceAverageSlot.State<Mapped_, Average_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractReferenceAverageSlot<Mapped_, Average_>
            implements UniConstraintCollectorValueHandle<A> {
        Slot(AbstractReferenceAverageSlot.State<Mapped_, Average_> state) {
            super(state);
        }

        @Override
        public void add(A a) {
            addMapped(mapper.apply(a));
        }

        @Override
        public void replaceWith(A a) {
            replaceWithMapped(mapper.apply(a));
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
        AverageReferenceUniCollector<?, ?, ?> that = (AverageReferenceUniCollector<?, ?, ?>) object;
        return Objects.equals(stateSupplier, that.stateSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), stateSupplier);
    }
}
