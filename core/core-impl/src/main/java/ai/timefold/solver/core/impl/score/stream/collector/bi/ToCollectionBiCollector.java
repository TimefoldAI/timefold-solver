package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.CustomCollectionUndoableActionable;

final class ToCollectionBiCollector<A, B, Mapped_, Result_ extends Collection<Mapped_>>
        extends UndoableActionableBiCollector<A, B, Mapped_, Result_, CustomCollectionUndoableActionable<Mapped_, Result_>> {
    private final IntFunction<Result_> collectionFunction;

    ToCollectionBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper,
            IntFunction<Result_> collectionFunction) {
        super(mapper);
        this.collectionFunction = collectionFunction;
    }

    @Override
    public Supplier<CustomCollectionUndoableActionable<Mapped_, Result_>> supplier() {
        return () -> new CustomCollectionUndoableActionable<>(collectionFunction);
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
