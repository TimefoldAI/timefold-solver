package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractToCollectionSlot;

import org.jspecify.annotations.NonNull;

final class ToCollectionBiCollector<A, B, Mapped_, Result_ extends Collection<Mapped_>>
        extends
        UndoableActionableBiCollector<A, B, Mapped_, Result_, AbstractToCollectionSlot.State<Mapped_, Result_>> {
    private final IntFunction<Result_> collectionFunction;

    ToCollectionBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper,
            IntFunction<Result_> collectionFunction) {
        super(mapper);
        this.collectionFunction = collectionFunction;
    }

    @Override
    public @NonNull Supplier<AbstractToCollectionSlot.State<Mapped_, Result_>> supplier() {
        return () -> new AbstractToCollectionSlot.State<>(collectionFunction);
    }

    @Override
    public @NonNull Function<AbstractToCollectionSlot.State<Mapped_, Result_>, Result_> finisher() {
        return state -> state.result();
    }

    @Override
    protected BiConstraintCollectorValueHandle<A, B> newAccumulatedValue(
            AbstractToCollectionSlot.State<Mapped_, Result_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractToCollectionSlot<Mapped_, Result_>
            implements BiConstraintCollectorValueHandle<A, B> {
        Slot(AbstractToCollectionSlot.State<Mapped_, Result_> state) {
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
        ToCollectionBiCollector<?, ?, ?, ?> that = (ToCollectionBiCollector<?, ?, ?, ?>) object;
        return Objects.equals(collectionFunction, that.collectionFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), collectionFunction);
    }
}
