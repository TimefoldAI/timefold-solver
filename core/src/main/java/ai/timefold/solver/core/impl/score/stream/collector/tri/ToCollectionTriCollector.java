package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Collection;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.collector.CustomCollectionUndoableActionable;

final class ToCollectionTriCollector<A, B, C, Mapped_, Result_ extends Collection<Mapped_>>
        extends
        UndoableActionableTriCollector<A, B, C, Mapped_, Result_, CustomCollectionUndoableActionable<Mapped_, Result_>> {
    private final IntFunction<Result_> collectionFunction;

    ToCollectionTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper,
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
        ToCollectionTriCollector<?, ?, ?, ?, ?> that = (ToCollectionTriCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(collectionFunction, that.collectionFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), collectionFunction);
    }
}
