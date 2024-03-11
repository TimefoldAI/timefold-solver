package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Collection;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.CustomCollectionUndoableActionable;

final class ToCollectionQuadCollector<A, B, C, D, Mapped_, Result_ extends Collection<Mapped_>>
        extends
        UndoableActionableQuadCollector<A, B, C, D, Mapped_, Result_, CustomCollectionUndoableActionable<Mapped_, Result_>> {
    private final IntFunction<Result_> collectionFunction;

    ToCollectionQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper,
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
        ToCollectionQuadCollector<?, ?, ?, ?, ?, ?> that = (ToCollectionQuadCollector<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(collectionFunction, that.collectionFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), collectionFunction);
    }
}
