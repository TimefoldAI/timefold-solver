package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractToCollectionSlot;

import org.jspecify.annotations.NonNull;

final class ToCollectionQuadCollector<A, B, C, D, Mapped_, Result_ extends Collection<Mapped_>>
        extends
        UndoableActionableQuadCollector<A, B, C, D, Mapped_, Result_, AbstractToCollectionSlot.State<Mapped_, Result_>> {
    private final IntFunction<Result_> collectionFunction;

    ToCollectionQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper,
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
    protected QuadConstraintCollectorAccumulatedValue<A, B, C, D> newAccumulatedValue(
            AbstractToCollectionSlot.State<Mapped_, Result_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractToCollectionSlot<Mapped_, Result_>
            implements QuadConstraintCollectorAccumulatedValue<A, B, C, D> {
        Slot(AbstractToCollectionSlot.State<Mapped_, Result_> state) {
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
        ToCollectionQuadCollector<?, ?, ?, ?, ?, ?> that = (ToCollectionQuadCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(collectionFunction, that.collectionFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), collectionFunction);
    }
}
