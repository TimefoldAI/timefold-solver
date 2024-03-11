package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.CustomCollectionUndoableActionable;

final class ToCollectionUniCollector<A, Mapped_, Result_ extends Collection<Mapped_>>
        extends UndoableActionableUniCollector<A, Mapped_, Result_, CustomCollectionUndoableActionable<Mapped_, Result_>> {
    private final IntFunction<Result_> collectionFunction;

    ToCollectionUniCollector(Function<? super A, ? extends Mapped_> mapper,
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
        ToCollectionUniCollector<?, ?, ?> that = (ToCollectionUniCollector<?, ?, ?>) object;
        return Objects.equals(collectionFunction, that.collectionFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), collectionFunction);
    }
}
